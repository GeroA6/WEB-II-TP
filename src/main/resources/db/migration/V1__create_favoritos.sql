CREATE TABLE favoritos (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    nota TEXT NOT NULL,
    fecha_alta TIMESTAMP NOT NULL
);