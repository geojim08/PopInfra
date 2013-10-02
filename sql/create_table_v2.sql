
drop table j_objet;
drop table j_element;

DROP INDEX MATRICE_TEXT_sx
DELETE FROM user_sdo_geom_metadata WHERE TABLE_NAME = 'J_OBJET';


drop CREATE SEQUENCE j_objet_seq;

delete from j_objet;
delete from j_element;

CREATE TABLE j_element
(
  ele_id     number NOT NULL,
  ecart_type number(10,3),
  GEOMETRY   SDO_GEOMETRY NOT NULL,
  CONSTRAINT regroup_point_pk PRIMARY KEY (ele_id)
);

CREATE TABLE j_objet
(
  obj_id    number NOT NULL,
  obj_type  number,
  obj_ele_id number,
  GEOMETRY  SDO_GEOMETRY NOT NULL,
  CONSTRAINT obj_id_pk PRIMARY KEY (obj_id),
  CONSTRAINT obj_ele_id_fk FOREIGN KEY (obj_ele_id)
      REFERENCES j_element (ele_id)
);


INSERT INTO user_sdo_geom_metadata
  (TABLE_NAME,
   COLUMN_NAME,
   DIMINFO,
   SRID)
  VALUES (
   'J_OBJET',
   'GEOMETRY',
   MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',  250000,  320000, 0.0005),
                       MDSYS.SDO_DIM_ELEMENT('Y', 5020000, 5070000, 0.0005)),
                       82196
);

INSERT INTO user_sdo_geom_metadata
  (TABLE_NAME,
   COLUMN_NAME,
   DIMINFO,
   SRID)
  VALUES (
   'J_ELEMENT',
   'GEOMETRY',
   MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',  250000,  320000, 0.0005),
                       MDSYS.SDO_DIM_ELEMENT('Y', 5020000, 5070000, 0.0005)),
                       82196
);

CREATE INDEX J_OBJET_sx ON J_OBJET(GEOMETRY) INDEXTYPE IS MDSYS.SPATIAL_INDEX
PARAMETERS('tablespace=XRIRE_SPATIAL');
CREATE INDEX J_ELEMENT_sx ON J_ELEMENT(GEOMETRY) INDEXTYPE IS MDSYS.SPATIAL_INDEX
PARAMETERS('tablespace=XRIRE_SPATIAL');

CREATE SEQUENCE j_element_seq
 START WITH     1
 INCREMENT BY   1
 NOCACHE
 NOCYCLE;

CREATE SEQUENCE j_objet_seq
 START WITH     1
 INCREMENT BY   1
 NOCACHE
 NOCYCLE;