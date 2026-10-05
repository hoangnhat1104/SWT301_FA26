CREATE TABLE roles (
    role_id INT PRIMARY KEY IDENTITY(1,1),
    role_name NVARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE users (
    user_id INT PRIMARY KEY IDENTITY(1,1),

    full_name NVARCHAR(100) NOT NULL,

    email NVARCHAR(255) NOT NULL UNIQUE,

    -- BCrypt password
    -- NULL nếu tài khoản đăng nhập bằng Google
    password_hash NVARCHAR(255),

    phone NVARCHAR(20),

    avatar_url NVARCHAR(500),

    -- PENDING / ACTIVE / LOCKED
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),

    updated_at DATETIME2,

    CONSTRAINT CK_users_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED'))
);
CREATE TABLE user_roles (
    user_id INT NOT NULL,

    role_id INT NOT NULL,

    assigned_at DATETIME2 NOT NULL DEFAULT GETDATE(),

    PRIMARY KEY (user_id, role_id),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    FOREIGN KEY (role_id)
        REFERENCES roles(role_id)
        ON DELETE CASCADE
);
CREATE TABLE email_verification_tokens (
    token_id INT PRIMARY KEY IDENTITY(1,1),

    user_id INT NOT NULL,

    token NVARCHAR(255) NOT NULL UNIQUE,

    expires_at DATETIME2 NOT NULL,

    verified_at DATETIME2,

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);
CREATE TABLE password_reset_tokens (
    token_id INT PRIMARY KEY IDENTITY(1,1),

    user_id INT NOT NULL,

    token NVARCHAR(255) NOT NULL UNIQUE,

    expires_at DATETIME2 NOT NULL,

    used_at DATETIME2,

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);
CREATE TABLE oauth_accounts (
    oauth_id INT PRIMARY KEY IDENTITY(1,1),

    user_id INT NOT NULL,

    provider NVARCHAR(50) NOT NULL,

    provider_user_id NVARCHAR(255) NOT NULL,

    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT UQ_oauth_provider_user
        UNIQUE (provider, provider_user_id),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);
INSERT INTO roles (role_name)
VALUES
    ('STUDENT'),
    ('INSTRUCTOR'),
    ('ADMINISTRATOR');