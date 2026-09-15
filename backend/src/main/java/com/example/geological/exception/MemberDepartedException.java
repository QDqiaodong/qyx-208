package com.example.geological.exception;

/**
 * 队员已停用（离队）。按队员编号查询所属小队资产、或对其做任何改挂/编辑时抛出。
 * 与「队员编号根本不存在」严格区分：编号存在但人已离队，必须明确告知已离队，
 * 不能笼统报「队员不存在」，更不能继续带出原小队操作台/承重等资产清单。
 */
public class MemberDepartedException extends RuntimeException {
    public MemberDepartedException(String message) {
        super(message);
    }
}
