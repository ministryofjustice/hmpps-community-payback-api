ALTER TABLE enforcement_actions ADD COLUMN respond_by_date_required BOOLEAN DEFAULT FALSE;

UPDATE enforcement_actions SET respond_by_date_required = TRUE WHERE code = 'ROM';
UPDATE enforcement_actions SET respond_by_date_required = TRUE WHERE code = 'EA02';
UPDATE enforcement_actions SET respond_by_date_required = TRUE WHERE code = 'EA03';
UPDATE enforcement_actions SET respond_by_date_required = TRUE WHERE code = 'EA05';
UPDATE enforcement_actions SET respond_by_date_required = TRUE WHERE code = 'EA08';
UPDATE enforcement_actions SET respond_by_date_required = TRUE WHERE code = 'EA12';