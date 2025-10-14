-- Script SQL pour ajouter la fonctionnalité de pools publics

-- 1. Ajouter la colonne is_public à la table Pool
ALTER TABLE Pool ADD COLUMN is_public BOOLEAN DEFAULT FALSE NOT NULL;

-- 2. Marquer les 3 premiers pools comme publics (pour la démo)
-- Tu peux changer les IDs selon tes pools existants
UPDATE Pool SET is_public = TRUE WHERE id IN (1, 2, 3);

-- 3. Vérifier les pools publics
SELECT id, name, description, is_public FROM Pool WHERE is_public = TRUE;
