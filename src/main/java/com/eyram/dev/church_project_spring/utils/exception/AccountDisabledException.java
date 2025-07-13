/**
 * CREATED BY @author GODWIN
 * DATE : 07/04/2025
 * YEAR : 2025
 * TIME : 17:46
 * PROJECT NAME : mef-rh-backend
 * CLASS : AccountDisabledException
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
 * TIME : 17:46
 * PROJECT NAME : mef-rh-backend
 * CLASS : AccountDisabledException
 * PACKAGE : tg.oddasolutions.mefrhbackend.exception
 * EMAIL: lordandre8@gmail.com
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
