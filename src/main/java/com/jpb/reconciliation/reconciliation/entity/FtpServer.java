package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Entity
@Data
@Table(name = "ftp_server")
public class FtpServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ftp_server_name", nullable = false)
    @NotBlank(message = "FTP Server name is required")
    private String ftpServerName;

    @Column(name = "server_ip", nullable = false)
    @NotBlank(message = "Server IP is required")
    private String serverIp;

    @Column(name = "port", nullable = false)
    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be >= 1")
    @Max(value = 65535, message = "Port must be <= 65535")
    private Integer port;

    @Column(name = "username", nullable = false)
    @NotBlank(message = "Username is required")
    private String userName;

    @Column(name = "password", nullable = false)
    @NotBlank(message = "Password is required")
    private String password;


}
