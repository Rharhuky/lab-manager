-- CampusLab Seed Data Migration
-- Inserts initial users for development/testing
--
-- Credentials:
--   professor@campus.edu.br / professor123  (role: PROFESSOR)
--   aluno@campus.edu.br     / aluno123      (role: ALUNO)

INSERT INTO users (name, email, password, role)
VALUES
    (
        'Professor Teste',
        'professor@campus.edu.br',
        '$2a$10$a55mHdTrgGPUo.F6ORWqyeJPtZPjyLjzCzCtrUirhQzQEDrDZPGdi',
        'PROFESSOR'
    ),
    (
        'Aluno Teste',
        'aluno@campus.edu.br',
        '$2a$10$tpRQZ.zmNZGNnuK0zk30FuG45qjRyJjiNc/cG/AC.2b9j0ICC5WPG',
        'ALUNO'
    );
