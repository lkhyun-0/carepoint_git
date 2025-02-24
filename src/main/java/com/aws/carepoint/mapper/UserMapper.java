package com.aws.carepoint.mapper;

import com.aws.carepoint.dto.FreeDto;
import com.aws.carepoint.dto.QnaDto;
import com.aws.carepoint.dto.UsersDto;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Mapper
public interface UserMapper {

    // 회원 가입 (ResultMap이 필요 없음)
    @Insert("INSERT INTO users (userid, username, userpwd, usernick, phone, email, auth_level, social_login_status, del_status) " +
            "VALUES (#{userId}, #{userName}, #{userPwd}, #{userNick}, #{phone}, #{email}, #{authLevel}, #{socialLoginStatus}, #{delStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "userPk", keyColumn = "user_pk")
    void insertUser(UsersDto usersDto);

    // 아이디 중복 체크 (단일 값 조회이므로 ResultMap 필요 없음)
    @Select("SELECT COUNT(*) FROM users WHERE userid = #{userId}")
    int countByUserId(@Param("userId") String userId);

    // 닉네임 중복 체크 (단일 값 조회이므로 ResultMap 필요 없음)
    @Select("SELECT COUNT(*) FROM users WHERE usernick = #{userNick}")
    int countByUserNick(@Param("userNick") String userNick);

    // `@Results(id = "userResultMap")`을 여기에 선언하여 한 번만 등록
    @Results(id = "userResultMap", value = {
            @Result(property = "userPk", column = "user_pk"),
            @Result(property = "authLevel", column = "auth_level"),
            @Result(property = "socialLoginStatus", column = "social_login_status"),
            @Result(property = "userId", column = "userid"),
            @Result(property = "userName", column = "username"),
            @Result(property = "userPwd", column = "userpwd"),
            @Result(property = "userNick", column = "usernick"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "email", column = "email"),
            @Result(property = "joinDate", column = "join_date"),
            @Result(property = "updateDate", column = "update_date"),
            @Result(property = "delStatus", column = "del_status"),
            @Result(property = "delDate", column = "delDate")
    })
    @Select("SELECT user_pk, userid, username, userpwd, usernick, phone, email, auth_level, social_login_status, del_status, delDate FROM users WHERE userid = #{userId}")
    UsersDto findByUserId(@Param("userId") String userId);

    // `@ResultMap("userResultMap")`을 사용하여 중복 제거
    @Select("SELECT * FROM users WHERE user_pk = #{userPk}")
    @ResultMap("userResultMap")
    UsersDto getUserById(int userPk);

    // 같은 이메일이 있는지 확인
    @Select("SELECT * FROM users WHERE email = #{email}")
    UsersDto findByEmail(String email);

    @Select("SELECT user_pk FROM users WHERE phone = #{phone}")
    String findPhoneByPhone(String phone);

    @Update("UPDATE users " +
            "SET phone = IFNULL(#{phone}, phone), " +
            "email = IFNULL(#{email}, email), " +
            "update_date = NOW() " +
            "WHERE user_pk = #{userPk}")
    int updateUserInfo(UsersDto usersDto);

    @Select("SELECT * FROM users WHERE username = #{userName} AND userId = #{userId} AND phone = #{phone} LIMIT 1")
    @ResultMap("userResultMap")
    UsersDto findUserByNameAndIdAndPhone(@Param("userName") String userName, @Param("userId") String userId, @Param("phone") String phone);

    @Update("UPDATE users SET userpwd = #{userPwd}, update_date = NOW() WHERE user_pk = #{userPk}")
    @ResultMap("userResultMap")
    int updateUserPassword(@Param("userPk") int userPk, @Param("userPwd") String password);

    @Select("SELECT user_pk, userId, userPwd, del_status FROM users WHERE user_pk = #{userPk} LIMIT 1")
    @ResultMap("userResultMap")
    UsersDto findByUserPk(@Param("userPk") int userPk);

    @Update("UPDATE users SET del_status = 1, delDate = NOW() WHERE user_pk = #{userPk}")
    int updateDelStatus(@Param("userPk") int userPk);

    @Update("UPDATE users SET userpwd = #{newPassword} WHERE user_pk = #{userPk}")
    void modifyUserPwd(@Param("userPk") int userPk, @Param("newPassword") String newPassword);


    @Select("SELECT phone FROM users WHERE phone IS NOT NULL")
    List<String> getAllUserPhoneNumbers();  // 모든 회원 전화번호 가져오기
}

