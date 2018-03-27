/*
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.testcoverageenforcer.ServerHandler;

import org.wso2.testcoverageenforcer.Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLServer {

    /**
     * SQL table
     */
    private ResultSet sqlTable;

    /**
     * connection with the sql server
     */
    private Connection m_connection;

    /**
     * Set table data containing WSO2 repos from the server
     */
    public SQLServer(String url, String username, String password) throws SQLException {

        this.m_connection = DriverManager.getConnection(
                url, username, password);

        PreparedStatement stat = this.m_connection.prepareStatement(
                "SELECT * FROM " + Constants.SQL_TABLE,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stat.setFetchSize(Integer.MIN_VALUE);
        this.sqlTable = stat.executeQuery();
    }

    /**
     * read next repository url
     */
    public String getNextRepositoryURL() throws SQLException {

        if (this.sqlTable.next()) {
            return this.sqlTable.getString(Constants.SQL_REPO_COLLUMN);
        } else {
            return null;
        }
    }

    /**
     * read next repository name. Exclude specific product category
     */
    public String getNextRepositoryName(char excludes) throws SQLException {

        if (this.sqlTable.next()) {
            if (!this.sqlTable.getString(Constants.SQL_REPO_PRODUCT).equals(excludes)) {
                return this.sqlTable.getString(Constants.SQL_REPO_COLLUMN).replace(Constants.GITHUB_URL, "");
            } else {
                return "";
            }
        } else {
            return null;
        }
    }

    /**
     * Close the sql connection
     */
    public void close() throws SQLException {

        this.m_connection.close();
    }
}
