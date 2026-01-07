package com.test;

// 指定泛型为 UserServiceImpl (或者你定义的任何模型类)
public class UserRepository extends BaseRepository<UserServiceImpl> {
    // 这里不需要写任何代码，构造函数会自动解析出 UserServiceImpl
}