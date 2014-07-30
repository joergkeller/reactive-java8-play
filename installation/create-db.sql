show databases;
create database books;
grant usage on *.* to books@localhost identified by 'books';
grant all privileges on books.* to books@localhost;
