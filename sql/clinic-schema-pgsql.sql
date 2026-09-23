-- PostgreSQL schema for Nautilus Clinic business tables.
-- Run after sql/ruoyi-pgsql.sql and sql/magic-api-pgsql.sql.
-- This file contains schema only; it intentionally has no patient/demo records.

CREATE SCHEMA IF NOT EXISTS ruoyi;

CREATE TABLE IF NOT EXISTS ruoyi.nautilus_patient (
    patient_id bigint NOT NULL,
    patient_name varchar(100) NOT NULL,
    gender char(1) DEFAULT '0',
    birth_date date,
    phone_number varchar(20),
    age integer,
    department varchar(64),
    dynamic_profile jsonb DEFAULT '{}'::jsonb,
    del_flag char(1) DEFAULT '0',
    create_by varchar(64) DEFAULT '',
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by varchar(64) DEFAULT '',
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    remark varchar(500),
    CONSTRAINT nautilus_patient_pkey PRIMARY KEY (patient_id),
    CONSTRAINT nautilus_patient_gender_check
        CHECK (gender = ANY (ARRAY['0'::bpchar, '1'::bpchar, '2'::bpchar]))
);

CREATE TABLE IF NOT EXISTS ruoyi.nautilus_consultation (
    consultation_id bigint NOT NULL,
    patient_id bigint NOT NULL,
    attending_doctor varchar(64),
    chief_complaint varchar(500),
    diagnosis varchar(2000),
    prescription_payload jsonb DEFAULT '[]'::jsonb,
    status char(1) DEFAULT '0',
    del_flag char(1) DEFAULT '0',
    create_by varchar(64) DEFAULT '',
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by varchar(64) DEFAULT '',
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    remark varchar(500),
    CONSTRAINT nautilus_consultation_pkey PRIMARY KEY (consultation_id)
);

CREATE TABLE IF NOT EXISTS ruoyi.nautilus_inventory (
    item_id bigint NOT NULL,
    item_code varchar(64) NOT NULL,
    item_name varchar(128) NOT NULL,
    category_dict varchar(64) NOT NULL,
    current_stock numeric(10,2) DEFAULT 0.00,
    ext_attributes jsonb DEFAULT '{}'::jsonb,
    del_flag char(1) DEFAULT '0',
    create_by varchar(64) DEFAULT '',
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by varchar(64) DEFAULT '',
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    remark varchar(500),
    alert_threshold integer DEFAULT 10,
    price numeric(10,2) DEFAULT 0.00,
    batch_no varchar(64),
    expiry_date date,
    CONSTRAINT nautilus_inventory_pkey PRIMARY KEY (item_id),
    CONSTRAINT uk_nautilus_item_code UNIQUE (item_code)
);

ALTER TABLE ruoyi.nautilus_consultation
    ALTER COLUMN attending_doctor DROP DEFAULT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'nautilus_inventory_current_stock_check'
          AND conrelid = 'ruoyi.nautilus_inventory'::regclass
    ) THEN
        ALTER TABLE ruoyi.nautilus_inventory
            ADD CONSTRAINT nautilus_inventory_current_stock_check
            CHECK (current_stock >= 0);
    END IF;
END $$;

COMMENT ON TABLE ruoyi.nautilus_patient IS '患者基础档案表';
COMMENT ON COLUMN ruoyi.nautilus_patient.dynamic_profile IS '患者动态病历/特征（JSONB扩展）';
COMMENT ON COLUMN ruoyi.nautilus_patient.age IS '年龄';
COMMENT ON COLUMN ruoyi.nautilus_patient.department IS '科别';
COMMENT ON TABLE ruoyi.nautilus_consultation IS '就诊与处方流转单';
COMMENT ON COLUMN ruoyi.nautilus_consultation.prescription_payload IS '处方药品快照（JSONB扩展数组）';
COMMENT ON TABLE ruoyi.nautilus_inventory IS '通用物资/药品库存表';
COMMENT ON COLUMN ruoyi.nautilus_inventory.ext_attributes IS '物资特殊属性（JSONB扩展）';
COMMENT ON COLUMN ruoyi.nautilus_inventory.batch_no IS '生产批次号';
COMMENT ON COLUMN ruoyi.nautilus_inventory.expiry_date IS '有效期至';

CREATE INDEX IF NOT EXISTS idx_patient_dynamic_profile
    ON ruoyi.nautilus_patient USING gin (dynamic_profile);
CREATE INDEX IF NOT EXISTS idx_consultation_patient
    ON ruoyi.nautilus_consultation USING btree (patient_id);
CREATE INDEX IF NOT EXISTS idx_inventory_ext_attributes
    ON ruoyi.nautilus_inventory USING gin (ext_attributes);
