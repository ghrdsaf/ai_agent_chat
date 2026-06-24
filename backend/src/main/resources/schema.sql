create table if not exists conversation_mapping (
    id bigserial primary key,
    channel_type varchar(64) not null,
    external_session_id varchar(255),
    external_user_id varchar(255),
    chatwoot_conversation_id bigint not null unique,
    internal_conversation_id bigint not null,
    product_id bigint,
    status varchar(32) not null
);

create table if not exists message (
    id bigserial primary key,
    conversation_id bigint not null,
    channel_type varchar(64) not null,
    sender_type varchar(32) not null,
    message_type varchar(32) not null,
    message_text varchar(4000) not null,
    message_time timestamp with time zone not null,
    fingerprint varchar(512) not null unique,
    process_status varchar(32) not null,
    risk_level varchar(32) not null
);
