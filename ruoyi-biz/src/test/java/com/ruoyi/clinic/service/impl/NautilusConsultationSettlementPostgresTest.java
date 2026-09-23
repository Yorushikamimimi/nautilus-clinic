package com.ruoyi.clinic.service.impl;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ruoyi.clinic.mapper.NautilusConsultationMapper;
import com.ruoyi.clinic.mapper.NautilusInventoryMapper;
import com.ruoyi.clinic.mapper.NautilusPatientMapper;
import com.ruoyi.clinic.service.IClinicBillingService;
import com.ruoyi.clinic.service.NautilusNotificationService;
import com.ruoyi.clinic.service.INautilusPatientService;
import com.ruoyi.common.exception.ServiceException;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** Uses PostgreSQL to exercise the conditional status transition and transaction rollback. */
@Testcontainers(disabledWithoutDocker = true)
class NautilusConsultationSettlementPostgresTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private DataSource dataSource;
    private NautilusConsultationMapper consultationMapper;
    private NautilusInventoryMapper inventoryMapper;
    private NautilusConsultationServiceImpl service;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource pgDataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        pgDataSource.setDriverClassName("org.postgresql.Driver");
        dataSource = pgDataSource;
        createTables();

        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();
        sqlSessionFactory.getConfiguration().addMapper(NautilusConsultationMapper.class);
        sqlSessionFactory.getConfiguration().addMapper(NautilusInventoryMapper.class);
        org.mybatis.spring.SqlSessionTemplate sessionTemplate =
                new org.mybatis.spring.SqlSessionTemplate(sqlSessionFactory);
        consultationMapper = sessionTemplate.getMapper(NautilusConsultationMapper.class);
        inventoryMapper = sessionTemplate.getMapper(NautilusInventoryMapper.class);

        NautilusInventoryServiceImpl inventoryService = new NautilusInventoryServiceImpl();
        ReflectionTestUtils.setField(inventoryService, "baseMapper", inventoryMapper);
        service = new NautilusConsultationServiceImpl(
                inventoryService, mock(NautilusNotificationService.class), mock(INautilusPatientService.class));
        ReflectionTestUtils.setField(service, "baseMapper", consultationMapper);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS ruoyi CASCADE");
            }
        }
    }

    @Test
    void repeatedConfirmationDeductsStockOnlyOnce() throws Exception {
        insertConsultation("[{\"itemCode\":\"RX-A\",\"quantity\":1}]");
        insertInventory("RX-A", 5);

        transactions.executeWithoutResult(status -> service.dispenseMedication(100L));
        assertThrows(ServiceException.class,
                () -> transactions.executeWithoutResult(status -> service.dispenseMedication(100L)));

        assertEquals("2", consultationStatus());
        assertEquals(4, stock("RX-A"));
    }

    @Test
    void concurrentConfirmationsHaveOneWinner() throws Exception {
        insertConsultation("[{\"itemCode\":\"RX-A\",\"quantity\":1}]");
        insertInventory("RX-A", 5);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> confirmAfter(start));
            Future<Boolean> second = pool.submit(() -> confirmAfter(start));
            start.countDown();
            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);

            assertEquals(1, successes);
            assertEquals("2", consultationStatus());
            assertEquals(4, stock("RX-A"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void insufficientLaterItemRollsBackStatusAndEarlierStockDeduction() throws Exception {
        insertConsultation("[{\"itemCode\":\"RX-A\",\"quantity\":1},{\"itemCode\":\"RX-B\",\"quantity\":2}]");
        insertInventory("RX-A", 5);
        insertInventory("RX-B", 1);

        assertThrows(ServiceException.class,
                () -> transactions.executeWithoutResult(status -> service.dispenseMedication(100L)));

        assertEquals("1", consultationStatus());
        assertEquals(5, stock("RX-A"));
        assertEquals(1, stock("RX-B"));
    }

    @Test
    void confirmationUsesConsultationIdWhenPatientHasTwoPendingVisits() throws Exception {
        insertConsultation(100L, 200L, "[{\"itemCode\":\"RX-A\",\"quantity\":1}]");
        insertConsultation(101L, 200L, "[{\"itemCode\":\"RX-B\",\"quantity\":2}]");
        insertInventory("RX-A", 5);
        insertInventory("RX-B", 5);
        IClinicBillingService billingService = new ClinicBillingServiceImpl(
                consultationMapper, mock(NautilusPatientMapper.class), service);

        transactions.executeWithoutResult(status -> billingService.confirmSimulatedSettlement(101L));

        assertEquals("1", consultationStatus(100L));
        assertEquals("2", consultationStatus(101L));
        assertEquals(5, stock("RX-A"));
        assertEquals(3, stock("RX-B"));
    }

    private boolean confirmAfter(CountDownLatch start) throws Exception {
        start.await();
        try {
            transactions.executeWithoutResult(status -> service.dispenseMedication(100L));
            return true;
        } catch (ServiceException expectedLoser) {
            return false;
        }
    }

    private void createTables() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA ruoyi");
            statement.execute("CREATE TABLE ruoyi.nautilus_consultation ("
                    + "consultation_id bigint PRIMARY KEY, patient_id bigint NOT NULL, attending_doctor varchar(64),"
                    + "chief_complaint varchar(500), diagnosis varchar(2000), prescription_payload jsonb, status char(1),"
                    + "del_flag char(1), create_by varchar(64), create_time timestamp, update_by varchar(64),"
                    + "update_time timestamp, remark varchar(500))");
            statement.execute("CREATE TABLE ruoyi.nautilus_inventory ("
                    + "item_id bigint PRIMARY KEY, item_code varchar(64) UNIQUE NOT NULL, item_name varchar(128) NOT NULL,"
                    + "category_dict varchar(64) NOT NULL, current_stock numeric(10,2) DEFAULT 0, ext_attributes jsonb,"
                    + "del_flag char(1), create_by varchar(64), create_time timestamp, update_by varchar(64),"
                    + "update_time timestamp, remark varchar(500), alert_threshold integer, price numeric(10,2),"
                    + "batch_no varchar(64), expiry_date date)");
        }
    }

    private void insertConsultation(String prescription) throws Exception {
        insertConsultation(100L, 200L, prescription);
    }

    private void insertConsultation(Long consultationId, Long patientId, String prescription) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO ruoyi.nautilus_consultation "
                    + "(consultation_id, patient_id, prescription_payload, status) VALUES (" + consultationId + ", "
                    + patientId + ", '"
                    + prescription + "'::jsonb, '1')");
        }
    }

    private void insertInventory(String itemCode, int count) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO ruoyi.nautilus_inventory "
                    + "(item_id, item_code, item_name, category_dict, current_stock) VALUES ("
                    + (itemCode.equals("RX-A") ? 1 : 2) + ", '" + itemCode + "', 'Drug', 'medicine', " + count + ")");
        }
    }

    private String consultationStatus() throws Exception {
        return consultationStatus(100L);
    }

    private String consultationStatus(Long consultationId) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             var result = statement.executeQuery("SELECT status FROM ruoyi.nautilus_consultation WHERE consultation_id=" + consultationId)) {
            result.next();
            return result.getString(1);
        }
    }

    private int stock(String itemCode) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             var result = statement.executeQuery("SELECT current_stock FROM ruoyi.nautilus_inventory WHERE item_code='" + itemCode + "'")) {
            result.next();
            return result.getInt(1);
        }
    }
}
