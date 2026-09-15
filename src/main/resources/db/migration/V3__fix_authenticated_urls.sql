-- Fix Cloudinary URLs: change /authenticated/ to /upload/ so images are publicly accessible
UPDATE photos SET original_url = REPLACE(original_url, '/authenticated/', '/upload/') WHERE original_url LIKE '%/authenticated/%';
UPDATE photos SET thumbnail_url = REPLACE(thumbnail_url, '/authenticated/', '/upload/') WHERE thumbnail_url LIKE '%/authenticated/%';
