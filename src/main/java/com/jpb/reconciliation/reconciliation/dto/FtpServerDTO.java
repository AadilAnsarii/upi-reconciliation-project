package com.jpb.reconciliation.reconciliation.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class FtpServerDTO {

    private Long id;

    @NotBlank(message = "FTP Server name is required")
    private String ftpServerName;

    @NotBlank(message = "Server IP is required")
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
             message = "Invalid IP address format")
    private String serverIp;

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be >= 1")
    @Max(value = 65535, message = "Port must be <= 65535")
    private Integer port;

    @NotBlank(message = "Username is required")
    private String userName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // Constructors
    public FtpServerDTO() {}

    public FtpServerDTO(Long id, String ftpServerName, String serverIp, Integer port, String userName, String password) {
        this.id = id;
        this.ftpServerName = ftpServerName;
        this.serverIp = serverIp;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFtpServerName() { return ftpServerName; }
    public void setFtpServerName(String ftpServerName) { this.ftpServerName = ftpServerName; }

    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

