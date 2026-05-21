-- Fix typo in PDU name
UPDATE community_campus_pdus
SET name = 'Birmingham Central'
WHERE name = 'Birmingham  Central';

UPDATE community_campus_pdus
SET provider_code = 'N53'
WHERE name IN (
    'East and West Lincolnshire',
    'Derbyshire',
    'Nottinghamshire',
    'Leicestershire'
);

UPDATE community_campus_pdus
SET provider_code = 'N56'
WHERE name IN (
    'Cambridgeshire',
    'South Essex',
    'Bedfordshire',
    'Suffolk',
    'North Essex',
    'Hertfordshire',
    'Northamptonshire',
    'Norfolk'
);

UPDATE community_campus_pdus
SET provider_code = 'N50'
WHERE name IN (
    'Bolton',
    'Bury',
    'Manchester North',
    'Manchester South',
    'Oldham',
    'Rochdale',
    'Salford',
    'Stockport',
    'Tameside',
    'Trafford',
    'Wigan'
);

UPDATE community_campus_pdus
SET provider_code = 'N57'
WHERE name IN (
    'East Kent',
    'East Sussex',
    'West Kent',
    'West Sussex',
    'Surrey'
);

UPDATE community_campus_pdus
SET provider_code = 'N07'
WHERE name IN (
    'Enfield and Haringey',
    'Haringey, Redbridge, Waltham Forest',
    'Lewisham and Bromley',
    'Lambeth and Wandsworth',
    'Ealing, Harrow, Hillingdon',
    'Barnet, Enfield and Brent'
);

UPDATE community_campus_pdus
SET provider_code = 'N54'
WHERE name IN (
    'Middlesbrough, Redcar and Cleveland',
    'North Tyneside and Northumberland',
    'County Durham and Darlington',
    'Sunderland',
    'Newcastle',
    'Gateshead and South Tyneside',
    'Stockton and Hartlepool'
);

UPDATE community_campus_pdus
SET provider_code = 'N51'
WHERE name IN (
    'Central Lancashire',
    'Cumbria',
    'Blackburn',
    'East Lancashire',
    'North Liverpool',
    'Cheshire West',
    'North West Lancashire',
    'Cheshire East',
    'Knowsley and St Helens',
    'Warrington and Halton',
    'Sefton',
    'Wirral'
);

UPDATE community_campus_pdus
SET provider_code = 'N59'
WHERE name IN (
    'Hampshire North and East',
    'Oxfordshire',
    'CP North AND Hampshire North and East',
    'Portsmouth and IOW',
    'Southampton, Eastleigh and New Forest',
    'Buckinghamshire and Milton Keynes',
    'West Berkshire',
    'East Berkshire'
);

UPDATE community_campus_pdus
SET provider_code = 'N58'
WHERE name IN (
    'Devon and Torbay',
    'Bath and North Somerset',
    'Cornwall and Isles of Scilly',
    'Dorset',
    'Bristol and South Gloucester',
    'Somerset',
    'Gloucestershire',
    'Swindon and Wiltshire',
    'Plymouth'
);

UPDATE community_campus_pdus
SET provider_code = 'N03'
WHERE name IN (
    'Dyfed Powys',
    'North Wales',
    'Cwm Taf Morgannwg',
    'Gwent',
    'Cardiff and Vale',
    'Swansea, Neath, Port Talbot'
);

UPDATE community_campus_pdus
SET provider_code = 'N52'
WHERE name IN (
    'Staffordshire and Stoke',
    'Birmingham South & Solihihull',
    'Birmingham Central',
    'Coventry',
    'Worcester, Kidderminster & Redditch',
    'Hereford',
    'South Warwickshire',
    'Black Country North, Walsall & Wolverhampton',
    'North Warwickshire & Rugby',
    'Birmingham North',
    'Tamworth',
    'Telford & Shrewsbury',
    'Black Country South Dudley & Sandwell'
);

UPDATE community_campus_pdus
SET provider_code = 'N55'
WHERE name IN (
    'Barnsley and Rotheram',
    'Kirklees',
    'Hull and East Riding',
    'Bradford and Calderdale',
    'Wakefield',
    'Sheffield',
    'Doncaster',
    'North Yorkshire',
    'North and North East Lincolnshire',
    'York',
    'Leeds'
);
