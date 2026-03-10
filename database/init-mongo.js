const dbName = process.env.MONGO_INITDB_DATABASE;
const dbUser = process.env.MONGO_INITDB_USERNAME;
const dbPwd = process.env.MONGO_INITDB_PASSWORD;

db = db.getSiblingDB(dbName);

db.createUser({
  user: dbUser,
  pwd: dbPwd,
  roles: [
    {
      role: 'readWrite',
      db: dbName,
    },
  ],
});

// Tạo một collection rỗng. 
// Trong MongoDB, database chỉ thực sự được tạo ra khi có dữ liệu (hoặc collection) bên trong nó.
db.createCollection('users');