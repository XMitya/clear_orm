A very lite SQLite ORM for Android which simpler to use directly in code, but possible to connect as external library (in a future). Without any licenses: you may use it and change as you wish.

Main goal is: simple, maybe poor ORM for Anroid projects which consists of minimal number of files that should be compiled directly to project. This should give a some basis, and possibility for improvement in some situations. 

ClearORM supports following basic operations:

-- create and update database;

-- insert;

-- select by ID;

-- select all;

-- update;

-- insert or update;

-- delete;

-- delete by ID (this two commands equivalen except that the first one extract ID from entity).

Entity maps with annotations: 

-- @SQLiteTable;

-- @SQLiteField.

Every entity should have id field. 

Supported field types:

-- int, Integer;

-- long, Long;

-- byte[];

-- String;

-- java.util.Date.

Date converts to string according to date parrern from annotation.

All entities classes must be enumerated in SQLiteHelper.entities array. This array will be used by SQLiteHelper for create tables, update database. You shouldn't forget assign name for database file in SQLiteHelper. You may annotate only fields, every field which annotated must have proper getters and setters, and entity must have default constructor.

There 2 ways for use Dao class:

-- create Dao by hands: 

	SQLiteHelper helper = new SQLiteHelper(context);

	Dao<MyEntity> dao = new Dao<MyEntity>(helper.getWritableDatabase(), MyEntity.class);

-- use DaoFactory:

	Dao<MyEntity> dao = DaoFactory.getInstance(context).getWriterDao(MyEntity.class);

Second solution is prefferable, because it caches all created Dao for prevent recreation of new objects; DaoFactory creates only one instance of SQLiteHelper class.

Hope this small project will be helpful for you and save your time. Any critics and help will be very appreciated.