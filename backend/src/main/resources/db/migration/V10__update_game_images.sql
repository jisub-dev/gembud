-- Update game images with placeholder URLs
-- Using placeholder.com for now - can be replaced with actual game images

UPDATE games SET image_url = 'https://placehold.co/600x800/1a1a2e/a855f7?text=League+of+Legends&font=montserrat'
WHERE name = 'League of Legends';

UPDATE games SET image_url = 'https://placehold.co/600x800/1a1a2e/06b6d4?text=PUBG&font=montserrat'
WHERE name = 'PUBG';

UPDATE games SET image_url = 'https://placehold.co/600x800/1a1a2e/ec4899?text=Valorant&font=montserrat'
WHERE name = 'Valorant';
