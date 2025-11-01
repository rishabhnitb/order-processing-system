-- Insert test customer
INSERT INTO customers (name, email, phone, active)
SELECT 'Test Customer', 'test@example.com', '+1234567890', true
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'test@example.com');

-- Insert test item
INSERT INTO items (name, price, description)
SELECT 'Test Item', 10.0, 'Test item description'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE name = 'Test Item');
