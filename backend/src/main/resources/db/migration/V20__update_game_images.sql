-- Update game images with real game images

UPDATE games SET image_url = 'https://ddragon.leagueoflegends.com/cdn/img/champion/splash/Ahri_0.jpg'
WHERE name = 'League of Legends';

UPDATE games SET image_url = 'https://cdn.steamstatic.com/steam/apps/578080/library_600x900.jpg'
WHERE name = 'PUBG';

UPDATE games SET image_url = 'https://static.wikia.nocookie.net/valorant/images/7/7a/Jett_VALORANT_Portrait.png/revision/latest'
WHERE name = 'Valorant';
