create table layouts (
	id integer generated always as identity primary key,
	name varchar(128) unique not null
);

create table pages (
	id integer generated always as identity primary key,
	name varchar(128) unique not null,
	layout_id integer references layouts(id)
);

create table widget_classes (
	id integer generated always as identity primary key,
	name varchar(128) unique not null
);

create table widgets (
	id integer generated always as identity primary key,
	class_id integer references widget_classes(id),
	page_id integer references pages(id),
	div_name varchar(128) not null
);

create table widget_properties (
	widget_id integer references widgets(id),
	name varchar(128) not null,
	value long varchar not null,
	primary key (widget_id, name)
);

create index widget_properties_name on widget_properties(name);

insert into layouts values( 'default' );

insert into pages values(
	'home',
	(select id from layouts where name = 'default')
);

insert into widget_classes values( 'HTMLWidget' );

insert into widgets (
	(select id from widget_classes 
);

end transaction;