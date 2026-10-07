ALTER TABLE favoritos
    ADD COLUMN lista_id BIGINT REFERENCES listas (id);