-- Rename ollama_description to mohe_description in place_descriptions table
ALTER TABLE place_descriptions RENAME COLUMN ollama_description TO mohe_description;
