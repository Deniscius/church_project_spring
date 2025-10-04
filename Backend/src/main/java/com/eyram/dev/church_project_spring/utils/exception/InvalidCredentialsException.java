/**
 * CREATED BY @author GODWIN
 * DATE : 07/04/2025
 * YEAR : 2025
 * TIME : 17:48
 * PROJECT NAME : mef-rh-backend
 * CLASS : InvalidCredentialsException
 * PACKAGE : tg.oddasolutions.mefrhbackend.exception
 * EMAIL: lordandre8@gmail.com
 */

package com.eyram.dev.church_project_spring.utils.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * CREATED BY @author GODWIN
 * DATE : 07/04/2025
 * YEAR : 2025
 * TIME : 17:48
 * PROJECT NAME : mef-rh-backend
 * CLASS : InvalidCredentialsException
 * PACKAGE : tg.oddasolutions.mefrhbackend.exception
 * EMAIL: lordandre8@gmail.com
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

}
