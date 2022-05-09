// This file is licensed under the Elastic License 2.0. Copyright 2021-present, StarRocks Limited.
package com.starrocks.sql.analyzer;

import com.starrocks.analysis.AdminSetConfigStmt;
import com.starrocks.analysis.AdminSetReplicaStatusStmt;
import com.starrocks.analysis.AlterSystemStmt;
import com.starrocks.analysis.AlterTableStmt;
import com.starrocks.analysis.AlterWorkGroupStmt;
import com.starrocks.analysis.BaseViewStmt;
import com.starrocks.analysis.CreateCatalogStmt;
import com.starrocks.analysis.CreateTableAsSelectStmt;
import com.starrocks.analysis.CreateWorkGroupStmt;
import com.starrocks.analysis.DeleteStmt;
import com.starrocks.analysis.DropCatalogStmt;
import com.starrocks.analysis.DropMaterializedViewStmt;
import com.starrocks.analysis.DropTableStmt;
import com.starrocks.analysis.InsertStmt;
import com.starrocks.analysis.LimitElement;
import com.starrocks.analysis.ShowStmt;
import com.starrocks.analysis.StatementBase;
import com.starrocks.analysis.UpdateStmt;
import com.starrocks.qe.ConnectContext;
import com.starrocks.qe.SessionVariable;
import com.starrocks.sql.ast.AnalyzeStmt;
import com.starrocks.sql.ast.AstVisitor;
import com.starrocks.sql.ast.BaseGrantRevokeRoleStmt;
import com.starrocks.sql.ast.CreateAnalyzeJobStmt;
import com.starrocks.sql.ast.QueryRelation;
import com.starrocks.sql.ast.QueryStatement;
import com.starrocks.sql.ast.SubmitTaskStmt;

public class Analyzer {
    public static void analyze(StatementBase statement, ConnectContext session) {
        new AnalyzerVisitor().analyze(statement, session);
    }

    private static class AnalyzerVisitor extends AstVisitor<Void, ConnectContext> {
        public void analyze(StatementBase statement, ConnectContext session) {
            statement.setClusterName(session.getClusterName());
            visit(statement, session);
        }

        @Override
        public Void visitAlterTableStatement(AlterTableStmt statement, ConnectContext context) {
            AlterTableStatementAnalyzer.analyze(statement, context);
            return null;
        }

        @Override
        public Void visitAlterWorkGroupStatement(AlterWorkGroupStmt statement, ConnectContext session) {
            statement.analyze();
            return null;
        }

        @Override
        public Void visitAnalyzeStatement(AnalyzeStmt statement, ConnectContext session) {
            AnalyzeStmtAnalyzer.analyze(statement, session);
            return null;
        }

        @Override
        public Void visitAdminSetReplicaStatusStatement(AdminSetReplicaStatusStmt statement, ConnectContext session) {
            AdminSetReplicaStatusStmtAnalyzer.analyze(statement, session);
            return null;
        }

        @Override
        public Void visitBaseViewStatement(BaseViewStmt statement, ConnectContext session) {
            ViewAnalyzer.analyze(statement, session);
            return null;
        }

        @Override
        public Void visitCreateAnalyzeJobStatement(CreateAnalyzeJobStmt statement, ConnectContext session) {
            AnalyzeStmtAnalyzer.analyze(statement, session);
            return null;
        }

        @Override
        public Void visitCreateTableAsSelectStatement(CreateTableAsSelectStmt statement, ConnectContext session) {
            // this phrase do not analyze insertStmt, insertStmt will analyze in
            // StmtExecutor.handleCreateTableAsSelectStmt because planner will not do meta operations
            CTASAnalyzer.transformCTASStmt((CreateTableAsSelectStmt) statement, session);
            return null;
        }

        @Override
        public Void visitSubmitTaskStmt(SubmitTaskStmt statement, ConnectContext context) {
            CreateTableAsSelectStmt createTableAsSelectStmt = statement.getCreateTableAsSelectStmt();
            QueryStatement queryStatement = createTableAsSelectStmt.getQueryStatement();
            Analyzer.analyze(queryStatement, context);
            String sqlText = "CREATE TABLE " +
                    createTableAsSelectStmt.getCreateTableStmt().getDbTbl().toSql() + " AS " +
                    ViewDefBuilder.build(queryStatement);
            statement.setSqlText(sqlText);
            TaskAnalyzer.analyzeSubmitTaskStmt(statement, context);
            return null;
        }

        @Override
        public Void visitCreateWorkGroupStatement(CreateWorkGroupStmt statement, ConnectContext session) {
            statement.analyze();
            return null;
        }

        @Override
        public Void visitInsertStatement(InsertStmt statement, ConnectContext session) {
            InsertAnalyzer.analyze(statement, session);
            return null;
        }

        @Override
        public Void visitShowStatement(ShowStmt statement, ConnectContext session) {
            ShowStmtAnalyzer.analyze(statement, session);
            return null;
        }

        @Override
        public Void visitAdminSetConfigStatement(AdminSetConfigStmt adminSetConfigStmt, ConnectContext session) {
            AdminSetStmtAnalyzer.analyze(adminSetConfigStmt, session);
            return null;
        }

        @Override
        public Void visitDropTableStmt(DropTableStmt statement, ConnectContext session) {
            DropStmtAnalyzer.analyze(statement, session);
            return null;
        }

        @Override
        public Void visitQueryStatement(QueryStatement stmt, ConnectContext session) {
            new QueryAnalyzer(session).analyze(stmt);

            QueryRelation queryRelation = stmt.getQueryRelation();
            long selectLimit = ConnectContext.get().getSessionVariable().getSqlSelectLimit();
            if (!queryRelation.hasLimit() && selectLimit != SessionVariable.DEFAULT_SELECT_LIMIT) {
                queryRelation.setLimit(new LimitElement(selectLimit));
            }
            return null;
        }

        @Override
        public Void visitUpdateStatement(UpdateStmt node, ConnectContext context) {
            UpdateAnalyzer.analyze(node, context);
            return null;
        }

        @Override
        public Void visitDeleteStatement(DeleteStmt node, ConnectContext context) {
            DeleteAnalyzer.analyze(node, context);
            return null;
        }

        @Override
        public Void visitGrantRevokeRoleStatement(BaseGrantRevokeRoleStmt stmt, ConnectContext session) {
            GrantRevokeRoleAnalyzer.analyze(stmt, session);
            return null;
        }

        @Override
        public Void visitDropMaterializedViewStatement(DropMaterializedViewStmt statement, ConnectContext context) {
            MaterializedViewAnalyzer.analyze(statement, context);
            return null;
        }

        @Override
        public Void visitAlterSystemStmt(AlterSystemStmt statement, ConnectContext context) {
            AlterSystemStmtAnalyzer.analyze(statement, context);
            return null;
        }

        @Override
        public Void visitCreateCatalogStatement(CreateCatalogStmt statement, ConnectContext context) {
            statement.analyze();
            return null;
        }

        @Override
        public Void visitDropCatalogStatement(DropCatalogStmt statement, ConnectContext context) {
            statement.analyze();
            return null;
        }
    }
}
