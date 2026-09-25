package com.dnd.ahaive.domain.insight.exception;

import com.dnd.ahaive.global.exception.CustomException;
import com.dnd.ahaive.global.exception.ErrorCode;

public class InitPieceDeleteNotAllowedException extends CustomException {

    public InitPieceDeleteNotAllowedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
