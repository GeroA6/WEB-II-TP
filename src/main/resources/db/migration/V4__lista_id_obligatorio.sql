-- 1. Crear la lista por defecto si no existe
INSERT INTO listas (nombre)
SELECT 'Sin clasificar'
WHERE NOT EXISTS (SELECT 1 FROM listas WHERE nombre = 'Sin clasificar');

-- 2. Asignar los favoritos existentes sin lista a la lista por defecto
UPDATE favoritos
SET lista_id = (SELECT id FROM listas WHERE nombre = 'Sin clasificar')
WHERE lista_id IS NULL;

-- 3. Ahora que no hay valores nulos, hacer la columna obligatoria
ALTER TABLE favoritos
ALTER COLUMN lista_id SET NOT NULL;