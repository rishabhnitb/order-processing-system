-- Insert sample customers if they don't exist
INSERT INTO customers (name, email, phone, active)
SELECT 'John Doe', 'john.doe@example.com', '+1234567890', true
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'john.doe@example.com');

INSERT INTO customers (name, email, phone, active)
SELECT 'Jane Smith', 'jane.smith@example.com', '+1987654321', true
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'jane.smith@example.com');

INSERT INTO customers (name, email, phone, active)
SELECT 'Alice Johnson', 'alice.j@example.com', '+1122334455', true
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE email = 'alice.j@example.com');

-- Insert sample items if they don't exist
INSERT INTO items (name, price, description)
SELECT 'Smartphone', 699.99, 'Latest model smartphone'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE name = 'Smartphone');

INSERT INTO items (name, price, description)
SELECT 'Laptop', 1299.99, 'High-performance laptop'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE name = 'Laptop');

INSERT INTO items (name, price, description)
SELECT 'Headphones', 199.99, 'Wireless noise-cancelling headphones'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE name = 'Headphones');

INSERT INTO items (name, price, description)
SELECT 'Tablet', 499.99, 'Professional tablet with stylus support'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE name = 'Tablet');

INSERT INTO items (name, price, description)
SELECT 'Smartwatch', 299.99, 'Fitness tracking smartwatch'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE name = 'Smartwatch');
