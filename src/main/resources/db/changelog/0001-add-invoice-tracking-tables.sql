-- liquibase formatted sql

-- changeset dev:0001-add-invoice-tracking-tables
CREATE TABLE invoices (
    id UUID NOT NULL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_invoices_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID', 'RECEIVED'))
);
-- rollback DROP TABLE invoices;

-- changeset dev:0001-add-invoice-recipients-table
CREATE TABLE invoice_recipients (
    id UUID NOT NULL PRIMARY KEY,
    invoice_id UUID NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(320),
    group_id VARCHAR(255),
    group_name VARCHAR(255),
    customer_reference VARCHAR(255),
    CONSTRAINT fk_invoice_recipients_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT chk_invoice_recipients_type
        CHECK (recipient_type IN ('PERSON', 'GROUP')),
    CONSTRAINT chk_invoice_recipients_person_email
        CHECK (
            recipient_type <> 'PERSON'
            OR email IS NOT NULL
        ),
    CONSTRAINT chk_invoice_recipients_group_reference
        CHECK (
            recipient_type <> 'GROUP'
            OR group_id IS NOT NULL
            OR group_name IS NOT NULL
        )
);
-- rollback DROP TABLE invoice_recipients;

-- changeset dev:0001-add-invoice-recipient-index
CREATE INDEX idx_invoice_recipients_invoice_id ON invoice_recipients (invoice_id);
-- rollback DROP INDEX idx_invoice_recipients_invoice_id;
