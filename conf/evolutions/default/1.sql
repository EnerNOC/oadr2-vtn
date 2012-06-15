# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table DURATIONPROPTYPE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  DURATION                  varchar(255),
  constraint pk_DURATIONPROPTYPE primary key (HJID))
;

create table EICREATEDEVENT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EIRESPONSE_EICREATEDEVENT_HJ_0 bigint,
  EVENTRESPONSES_EICREATEDEVEN_0 bigint,
  VENID                     varchar(255),
  constraint pk_EICREATEDEVENT primary key (HJID))
;

create table EIEVENT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EVENTDESCRIPTOR_EIEVENT_HJID bigint,
  EIACTIVEPERIOD_EIEVENT_HJID bigint,
  EIEVENTSIGNALS_EIEVENT_HJID bigint,
  EITARGET_EIEVENT_HJID     bigint,
  constraint pk_EIEVENT primary key (HJID))
;

create table EIACTIVEPERIOD (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  PROPERTIES_EIACTIVEPERIOD_HJ_0 bigint,
  constraint pk_EIACTIVEPERIOD primary key (HJID))
;

create table EIEVENTSIGNALS (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  constraint pk_EIEVENTSIGNALS primary key (HJID))
;

create table EIEVENTSIGNAL (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EIEVENTSIGNAL_EIEVENTSIGNALS_0 bigint not null,
  INTERVALS_EIEVENTSIGNAL_HJID bigint,
  SIGNALNAME                varchar(255),
  SIGNALTYPE                varchar(16),
  SIGNALID                  varchar(255),
  CURRENTVALUE_EIEVENTSIGNAL_H_0 bigint,
  constraint ck_EIEVENTSIGNAL_SIGNALTYPE check (SIGNALTYPE in ('DELTA','LEVEL','MULTIPLIER','PRICE','PRICE_MULTIPLIER','PRICE_RELATIVE','PRODUCT','SETPOINT')),
  constraint pk_EIEVENTSIGNAL primary key (HJID))
;

create table CURRENTVALUE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  PAYLOADFLOAT_CURRENTVALUE_HJ_0 bigint,
  constraint pk_CURRENTVALUE primary key (HJID))
;

create table EITARGET (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  constraint pk_EITARGET primary key (HJID))
;

create table EITARGETGROUPIDITEM (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  ITEM                      varchar(255),
  constraint pk_EITARGETGROUPIDITEM primary key (HJID))
;

create table EITARGETPARTYIDITEM (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  ITEM                      varchar(255),
  constraint pk_EITARGETPARTYIDITEM primary key (HJID))
;

create table EITARGETRESOURCEIDITEM (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  ITEM                      varchar(255),
  constraint pk_EITARGETRESOURCEIDITEM primary key (HJID))
;

create table EITARGETVENIDITEM (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  ITEM                      varchar(255),
  constraint pk_EITARGETVENIDITEM primary key (HJID))
;

create table EVENTDESCRIPTOR (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EVENTID                   varchar(255),
  MODIFICATIONNUMBER        bigint(10),
  PRIORITY                  bigint(10),
  EIMARKETCONTEXT_EVENTDESCRIP_0 bigint,
  EVENTSTATUS               varchar(9),
  TESTEVENT                 varchar(255),
  VTNCOMMENT                varchar(255),
  constraint ck_EVENTDESCRIPTOR_EVENTSTATUS check (EVENTSTATUS in ('NONE','FAR','NEAR','ACTIVE','COMPLETED','CANCELLED')),
  constraint pk_EVENTDESCRIPTOR primary key (HJID))
;

create table EIMARKETCONTEXT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  MARKETCONTEXT             varchar(255),
  constraint pk_EIMARKETCONTEXT primary key (HJID))
;

create table EIREQUESTEVENT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  REQUESTID                 varchar(255),
  VENID                     varchar(255),
  REPLYLIMIT                bigint(10),
  constraint pk_EIREQUESTEVENT primary key (HJID))
;

create table EIRESPONSE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  RESPONSECODE              varchar(255),
  RESPONSEDESCRIPTION       varchar(255),
  REQUESTID                 varchar(255),
  constraint pk_EIRESPONSE primary key (HJID))
;

create table EVENTRESPONSES (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  constraint pk_EVENTRESPONSES primary key (HJID))
;

create table EVENTRESPONSE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EVENTRESPONSE_EVENTRESPONSES_0 bigint not null,
  RESPONSECODE              varchar(255),
  RESPONSEDESCRIPTION       varchar(255),
  REQUESTID                 varchar(255),
  QUALIFIEDEVENTID_EVENTRESPON_0 bigint,
  OPTTYPE                   varchar(7),
  constraint ck_EVENTRESPONSE_OPTTYPE check (OPTTYPE in ('OPT_IN','OPT_OUT')),
  constraint pk_EVENTRESPONSE primary key (HJID))
;

create table INTERVAL_ (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  INTERVAL__INTERVALS_HJID  bigint not null,
  DURATION_INTERVAL__HJID   bigint,
  UID__INTERVAL__HJID       bigint,
  SIGNALPAYLOAD_INTERVAL__HJID bigint,
  constraint pk_INTERVAL_ primary key (HJID))
;

create table SIGNALPAYLOAD (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  PAYLOADFLOAT_SIGNALPAYLOAD_H_0 bigint,
  constraint pk_SIGNALPAYLOAD primary key (HJID))
;

create table INTERVALS (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  constraint pk_INTERVALS primary key (HJID))
;

create table OADRCREATEDEVENT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EICREATEDEVENT_OADRCREATEDEV_0 bigint,
  constraint pk_OADRCREATEDEVENT primary key (HJID))
;

create table OADRDISTRIBUTEEVENT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EIRESPONSE_OADRDISTRIBUTEEVE_0 bigint,
  REQUESTID                 varchar(255),
  VTNID                     varchar(255),
  constraint pk_OADRDISTRIBUTEEVENT primary key (HJID))
;

create table OADREVENT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  OADREVENT_OADRDISTRIBUTEEVEN_0 bigint not null,
  EIEVENT_OADREVENT_HJID    bigint,
  OADRRESPONSEREQUIRED      varchar(6),
  constraint ck_OADREVENT_OADRRESPONSEREQUIRED check (OADRRESPONSEREQUIRED in ('ALWAYS','NEVER')),
  constraint pk_OADREVENT primary key (HJID))
;

create table OADRREQUESTEVENT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EIREQUESTEVENT_OADRREQUESTEV_0 bigint,
  constraint pk_OADRREQUESTEVENT primary key (HJID))
;

create table OADRRESPONSE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EIRESPONSE_OADRRESPONSE_HJID bigint,
  constraint pk_OADRRESPONSE primary key (HJID))
;

create table PAYLOADFLOAT (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  VALUE_                    float(20,10),
  constraint pk_PAYLOADFLOAT primary key (HJID))
;

create table PROPERTIES (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  DTSTART_PROPERTIES_HJID   bigint,
  DURATION_PROPERTIES_HJID  bigint,
  TOLERANCE_PROPERTIES_HJID bigint,
  XEINOTIFICATION_PROPERTIES_H_0 bigint,
  XEIRAMPUP_PROPERTIES_HJID bigint,
  XEIRECOVERY_PROPERTIES_HJID bigint,
  constraint pk_PROPERTIES primary key (HJID))
;

create table DTSTART (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  constraint pk_DTSTART primary key (HJID))
;

create table TOLERANCE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  TOLERATE_TOLERANCE_HJID   bigint,
  constraint pk_TOLERANCE primary key (HJID))
;

create table TOLERATE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  STARTBEFORE               varchar(255),
  STARTAFTER                varchar(255),
  constraint pk_TOLERATE primary key (HJID))
;

create table QUALIFIEDEVENTIDTYPE (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  EVENTID                   varchar(255),
  MODIFICATIONNUMBER        bigint(10),
  constraint pk_QUALIFIEDEVENTIDTYPE primary key (HJID))
;

create table UID_ (
  dtype                     varchar(10) not null,
  HJID                      bigint not null,
  TEXT                      varchar(255),
  constraint pk_UID_ primary key (HJID))
;

create sequence DURATIONPROPTYPE_seq;

create sequence EICREATEDEVENT_seq;

create sequence EIEVENT_seq;

create sequence EIACTIVEPERIOD_seq;

create sequence EIEVENTSIGNALS_seq;

create sequence EIEVENTSIGNAL_seq;

create sequence CURRENTVALUE_seq;

create sequence EITARGET_seq;

create sequence EITARGETGROUPIDITEM_seq;

create sequence EITARGETPARTYIDITEM_seq;

create sequence EITARGETRESOURCEIDITEM_seq;

create sequence EITARGETVENIDITEM_seq;

create sequence EVENTDESCRIPTOR_seq;

create sequence EIMARKETCONTEXT_seq;

create sequence EIREQUESTEVENT_seq;

create sequence EIRESPONSE_seq;

create sequence EVENTRESPONSES_seq;

create sequence EVENTRESPONSE_seq;

create sequence INTERVAL__seq;

create sequence SIGNALPAYLOAD_seq;

create sequence INTERVALS_seq;

create sequence OADRCREATEDEVENT_seq;

create sequence OADRDISTRIBUTEEVENT_seq;

create sequence OADREVENT_seq;

create sequence OADRREQUESTEVENT_seq;

create sequence OADRRESPONSE_seq;

create sequence PAYLOADFLOAT_seq;

create sequence PROPERTIES_seq;

create sequence DTSTART_seq;

create sequence TOLERANCE_seq;

create sequence TOLERATE_seq;

create sequence QUALIFIEDEVENTIDTYPE_seq;

create sequence UID__seq;

alter table EICREATEDEVENT add constraint fk_EICREATEDEVENT_eiResponse_1 foreign key (EIRESPONSE_EICREATEDEVENT_HJ_0) references EIRESPONSE (HJID) on delete restrict on update restrict;
create index ix_EICREATEDEVENT_eiResponse_1 on EICREATEDEVENT (EIRESPONSE_EICREATEDEVENT_HJ_0);
alter table EICREATEDEVENT add constraint fk_EICREATEDEVENT_eventRespons_2 foreign key (EVENTRESPONSES_EICREATEDEVEN_0) references EVENTRESPONSES (HJID) on delete restrict on update restrict;
create index ix_EICREATEDEVENT_eventRespons_2 on EICREATEDEVENT (EVENTRESPONSES_EICREATEDEVEN_0);
alter table EIEVENT add constraint fk_EIEVENT_eventDescriptor_3 foreign key (EVENTDESCRIPTOR_EIEVENT_HJID) references EVENTDESCRIPTOR (HJID) on delete restrict on update restrict;
create index ix_EIEVENT_eventDescriptor_3 on EIEVENT (EVENTDESCRIPTOR_EIEVENT_HJID);
alter table EIEVENT add constraint fk_EIEVENT_eiActivePeriod_4 foreign key (EIACTIVEPERIOD_EIEVENT_HJID) references EIACTIVEPERIOD (HJID) on delete restrict on update restrict;
create index ix_EIEVENT_eiActivePeriod_4 on EIEVENT (EIACTIVEPERIOD_EIEVENT_HJID);
alter table EIEVENT add constraint fk_EIEVENT_eiEventSignals_5 foreign key (EIEVENTSIGNALS_EIEVENT_HJID) references EIEVENTSIGNALS (HJID) on delete restrict on update restrict;
create index ix_EIEVENT_eiEventSignals_5 on EIEVENT (EIEVENTSIGNALS_EIEVENT_HJID);
alter table EIEVENT add constraint fk_EIEVENT_eiTarget_6 foreign key (EITARGET_EIEVENT_HJID) references EITARGET (HJID) on delete restrict on update restrict;
create index ix_EIEVENT_eiTarget_6 on EIEVENT (EITARGET_EIEVENT_HJID);
alter table EIACTIVEPERIOD add constraint fk_EIACTIVEPERIOD_properties_7 foreign key (PROPERTIES_EIACTIVEPERIOD_HJ_0) references PROPERTIES (HJID) on delete restrict on update restrict;
create index ix_EIACTIVEPERIOD_properties_7 on EIACTIVEPERIOD (PROPERTIES_EIACTIVEPERIOD_HJ_0);
alter table EIEVENTSIGNAL add constraint fk_EIEVENTSIGNAL_EIEVENTSIGNAL_8 foreign key (EIEVENTSIGNAL_EIEVENTSIGNALS_0) references EIEVENTSIGNALS (HJID) on delete restrict on update restrict;
create index ix_EIEVENTSIGNAL_EIEVENTSIGNAL_8 on EIEVENTSIGNAL (EIEVENTSIGNAL_EIEVENTSIGNALS_0);
alter table EIEVENTSIGNAL add constraint fk_EIEVENTSIGNAL_intervals_9 foreign key (INTERVALS_EIEVENTSIGNAL_HJID) references INTERVALS (HJID) on delete restrict on update restrict;
create index ix_EIEVENTSIGNAL_intervals_9 on EIEVENTSIGNAL (INTERVALS_EIEVENTSIGNAL_HJID);
alter table EIEVENTSIGNAL add constraint fk_EIEVENTSIGNAL_currentValue_10 foreign key (CURRENTVALUE_EIEVENTSIGNAL_H_0) references CURRENTVALUE (HJID) on delete restrict on update restrict;
create index ix_EIEVENTSIGNAL_currentValue_10 on EIEVENTSIGNAL (CURRENTVALUE_EIEVENTSIGNAL_H_0);
alter table CURRENTVALUE add constraint fk_CURRENTVALUE_payloadFloat_11 foreign key (PAYLOADFLOAT_CURRENTVALUE_HJ_0) references PAYLOADFLOAT (HJID) on delete restrict on update restrict;
create index ix_CURRENTVALUE_payloadFloat_11 on CURRENTVALUE (PAYLOADFLOAT_CURRENTVALUE_HJ_0);
alter table EVENTDESCRIPTOR add constraint fk_EVENTDESCRIPTOR_eiMarketCo_12 foreign key (EIMARKETCONTEXT_EVENTDESCRIP_0) references EIMARKETCONTEXT (HJID) on delete restrict on update restrict;
create index ix_EVENTDESCRIPTOR_eiMarketCo_12 on EVENTDESCRIPTOR (EIMARKETCONTEXT_EVENTDESCRIP_0);
alter table EVENTRESPONSE add constraint fk_EVENTRESPONSE_EVENTRESPONS_13 foreign key (EVENTRESPONSE_EVENTRESPONSES_0) references EVENTRESPONSES (HJID) on delete restrict on update restrict;
create index ix_EVENTRESPONSE_EVENTRESPONS_13 on EVENTRESPONSE (EVENTRESPONSE_EVENTRESPONSES_0);
alter table EVENTRESPONSE add constraint fk_EVENTRESPONSE_qualifiedEve_14 foreign key (QUALIFIEDEVENTID_EVENTRESPON_0) references QUALIFIEDEVENTIDTYPE (HJID) on delete restrict on update restrict;
create index ix_EVENTRESPONSE_qualifiedEve_14 on EVENTRESPONSE (QUALIFIEDEVENTID_EVENTRESPON_0);
alter table INTERVAL_ add constraint fk_INTERVAL__INTERVALS_15 foreign key (INTERVAL__INTERVALS_HJID) references INTERVALS (HJID) on delete restrict on update restrict;
create index ix_INTERVAL__INTERVALS_15 on INTERVAL_ (INTERVAL__INTERVALS_HJID);
alter table INTERVAL_ add constraint fk_INTERVAL__duration_16 foreign key (DURATION_INTERVAL__HJID) references DURATIONPROPTYPE (HJID) on delete restrict on update restrict;
create index ix_INTERVAL__duration_16 on INTERVAL_ (DURATION_INTERVAL__HJID);
alter table INTERVAL_ add constraint fk_INTERVAL__uid_17 foreign key (UID__INTERVAL__HJID) references UID_ (HJID) on delete restrict on update restrict;
create index ix_INTERVAL__uid_17 on INTERVAL_ (UID__INTERVAL__HJID);
alter table INTERVAL_ add constraint fk_INTERVAL__signalPayload_18 foreign key (SIGNALPAYLOAD_INTERVAL__HJID) references SIGNALPAYLOAD (HJID) on delete restrict on update restrict;
create index ix_INTERVAL__signalPayload_18 on INTERVAL_ (SIGNALPAYLOAD_INTERVAL__HJID);
alter table SIGNALPAYLOAD add constraint fk_SIGNALPAYLOAD_payloadFloat_19 foreign key (PAYLOADFLOAT_SIGNALPAYLOAD_H_0) references PAYLOADFLOAT (HJID) on delete restrict on update restrict;
create index ix_SIGNALPAYLOAD_payloadFloat_19 on SIGNALPAYLOAD (PAYLOADFLOAT_SIGNALPAYLOAD_H_0);
alter table OADRCREATEDEVENT add constraint fk_OADRCREATEDEVENT_eiCreated_20 foreign key (EICREATEDEVENT_OADRCREATEDEV_0) references EICREATEDEVENT (HJID) on delete restrict on update restrict;
create index ix_OADRCREATEDEVENT_eiCreated_20 on OADRCREATEDEVENT (EICREATEDEVENT_OADRCREATEDEV_0);
alter table OADRDISTRIBUTEEVENT add constraint fk_OADRDISTRIBUTEEVENT_eiResp_21 foreign key (EIRESPONSE_OADRDISTRIBUTEEVE_0) references EIRESPONSE (HJID) on delete restrict on update restrict;
create index ix_OADRDISTRIBUTEEVENT_eiResp_21 on OADRDISTRIBUTEEVENT (EIRESPONSE_OADRDISTRIBUTEEVE_0);
alter table OADREVENT add constraint fk_OADREVENT_OADRDISTRIBUTEEV_22 foreign key (OADREVENT_OADRDISTRIBUTEEVEN_0) references OADRDISTRIBUTEEVENT (HJID) on delete restrict on update restrict;
create index ix_OADREVENT_OADRDISTRIBUTEEV_22 on OADREVENT (OADREVENT_OADRDISTRIBUTEEVEN_0);
alter table OADREVENT add constraint fk_OADREVENT_eiEvent_23 foreign key (EIEVENT_OADREVENT_HJID) references EIEVENT (HJID) on delete restrict on update restrict;
create index ix_OADREVENT_eiEvent_23 on OADREVENT (EIEVENT_OADREVENT_HJID);
alter table OADRREQUESTEVENT add constraint fk_OADRREQUESTEVENT_eiRequest_24 foreign key (EIREQUESTEVENT_OADRREQUESTEV_0) references EIREQUESTEVENT (HJID) on delete restrict on update restrict;
create index ix_OADRREQUESTEVENT_eiRequest_24 on OADRREQUESTEVENT (EIREQUESTEVENT_OADRREQUESTEV_0);
alter table OADRRESPONSE add constraint fk_OADRRESPONSE_eiResponse_25 foreign key (EIRESPONSE_OADRRESPONSE_HJID) references EIRESPONSE (HJID) on delete restrict on update restrict;
create index ix_OADRRESPONSE_eiResponse_25 on OADRRESPONSE (EIRESPONSE_OADRRESPONSE_HJID);
alter table PROPERTIES add constraint fk_PROPERTIES_dtstart_26 foreign key (DTSTART_PROPERTIES_HJID) references DTSTART (HJID) on delete restrict on update restrict;
create index ix_PROPERTIES_dtstart_26 on PROPERTIES (DTSTART_PROPERTIES_HJID);
alter table PROPERTIES add constraint fk_PROPERTIES_duration_27 foreign key (DURATION_PROPERTIES_HJID) references DURATIONPROPTYPE (HJID) on delete restrict on update restrict;
create index ix_PROPERTIES_duration_27 on PROPERTIES (DURATION_PROPERTIES_HJID);
alter table PROPERTIES add constraint fk_PROPERTIES_tolerance_28 foreign key (TOLERANCE_PROPERTIES_HJID) references TOLERANCE (HJID) on delete restrict on update restrict;
create index ix_PROPERTIES_tolerance_28 on PROPERTIES (TOLERANCE_PROPERTIES_HJID);
alter table PROPERTIES add constraint fk_PROPERTIES_xEiNotification_29 foreign key (XEINOTIFICATION_PROPERTIES_H_0) references DURATIONPROPTYPE (HJID) on delete restrict on update restrict;
create index ix_PROPERTIES_xEiNotification_29 on PROPERTIES (XEINOTIFICATION_PROPERTIES_H_0);
alter table PROPERTIES add constraint fk_PROPERTIES_xEiRampUp_30 foreign key (XEIRAMPUP_PROPERTIES_HJID) references DURATIONPROPTYPE (HJID) on delete restrict on update restrict;
create index ix_PROPERTIES_xEiRampUp_30 on PROPERTIES (XEIRAMPUP_PROPERTIES_HJID);
alter table PROPERTIES add constraint fk_PROPERTIES_xEiRecovery_31 foreign key (XEIRECOVERY_PROPERTIES_HJID) references DURATIONPROPTYPE (HJID) on delete restrict on update restrict;
create index ix_PROPERTIES_xEiRecovery_31 on PROPERTIES (XEIRECOVERY_PROPERTIES_HJID);
alter table TOLERANCE add constraint fk_TOLERANCE_tolerate_32 foreign key (TOLERATE_TOLERANCE_HJID) references TOLERATE (HJID) on delete restrict on update restrict;
create index ix_TOLERANCE_tolerate_32 on TOLERANCE (TOLERATE_TOLERANCE_HJID);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists DURATIONPROPTYPE;

drop table if exists EICREATEDEVENT;

drop table if exists EIEVENT;

drop table if exists EIACTIVEPERIOD;

drop table if exists EIEVENTSIGNALS;

drop table if exists EIEVENTSIGNAL;

drop table if exists CURRENTVALUE;

drop table if exists EITARGET;

drop table if exists EITARGETGROUPIDITEM;

drop table if exists EITARGETPARTYIDITEM;

drop table if exists EITARGETRESOURCEIDITEM;

drop table if exists EITARGETVENIDITEM;

drop table if exists EVENTDESCRIPTOR;

drop table if exists EIMARKETCONTEXT;

drop table if exists EIREQUESTEVENT;

drop table if exists EIRESPONSE;

drop table if exists EVENTRESPONSES;

drop table if exists EVENTRESPONSE;

drop table if exists INTERVAL_;

drop table if exists SIGNALPAYLOAD;

drop table if exists INTERVALS;

drop table if exists OADRCREATEDEVENT;

drop table if exists OADRDISTRIBUTEEVENT;

drop table if exists OADREVENT;

drop table if exists OADRREQUESTEVENT;

drop table if exists OADRRESPONSE;

drop table if exists PAYLOADFLOAT;

drop table if exists PROPERTIES;

drop table if exists DTSTART;

drop table if exists TOLERANCE;

drop table if exists TOLERATE;

drop table if exists QUALIFIEDEVENTIDTYPE;

drop table if exists UID_;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists DURATIONPROPTYPE_seq;

drop sequence if exists EICREATEDEVENT_seq;

drop sequence if exists EIEVENT_seq;

drop sequence if exists EIACTIVEPERIOD_seq;

drop sequence if exists EIEVENTSIGNALS_seq;

drop sequence if exists EIEVENTSIGNAL_seq;

drop sequence if exists CURRENTVALUE_seq;

drop sequence if exists EITARGET_seq;

drop sequence if exists EITARGETGROUPIDITEM_seq;

drop sequence if exists EITARGETPARTYIDITEM_seq;

drop sequence if exists EITARGETRESOURCEIDITEM_seq;

drop sequence if exists EITARGETVENIDITEM_seq;

drop sequence if exists EVENTDESCRIPTOR_seq;

drop sequence if exists EIMARKETCONTEXT_seq;

drop sequence if exists EIREQUESTEVENT_seq;

drop sequence if exists EIRESPONSE_seq;

drop sequence if exists EVENTRESPONSES_seq;

drop sequence if exists EVENTRESPONSE_seq;

drop sequence if exists INTERVAL__seq;

drop sequence if exists SIGNALPAYLOAD_seq;

drop sequence if exists INTERVALS_seq;

drop sequence if exists OADRCREATEDEVENT_seq;

drop sequence if exists OADRDISTRIBUTEEVENT_seq;

drop sequence if exists OADREVENT_seq;

drop sequence if exists OADRREQUESTEVENT_seq;

drop sequence if exists OADRRESPONSE_seq;

drop sequence if exists PAYLOADFLOAT_seq;

drop sequence if exists PROPERTIES_seq;

drop sequence if exists DTSTART_seq;

drop sequence if exists TOLERANCE_seq;

drop sequence if exists TOLERATE_seq;

drop sequence if exists QUALIFIEDEVENTIDTYPE_seq;

drop sequence if exists UID__seq;

