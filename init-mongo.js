const dbName = process.env.SPRING_MONGODB_DATABASE;
const dbUser = process.env.SPRING_MONGODB_USERNAME;
const dbPwd = process.env.SPRING_MONGODB_PASSWORD;

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