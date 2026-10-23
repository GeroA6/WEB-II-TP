ALTER TABLE listas
ADD COLUMN usuario_id BIGINT REFERENCES usuarios(id);