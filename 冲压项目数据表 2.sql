--冲压信息表，包含了（导入表、pad显示）的部分内容。

CREATE TABLE	冲压部品信息表(		
id	int	NOT NULL PRIMARY KEY IDENTITY,--流水号
机器名	 nvarchar	(20),
订单编号  nvarchar	(40),	
物料编号  nvarchar	(40),--(图番)	
品名	 nvarchar	(100),
材料 	 nvarchar	(100),
日计划数 int,
加工完成数 int,
设备转速  decimal(6,4),--0.0001
工时  decimal(6,1),	
ST	 decimal(6,4),
计划日期 datetime,
加工人员ID nvarchar	(50),
完工日期  nvarchar	(50)
)


CREATE TABLE	冲压设备状态表(		
机器名	nvarchar	(20) NOT NULL PRIMARY KEY ,	
工作时间 nvarchar 	(100),
状态	nvarchar	(20),--红，黄，绿
当前加工id int,
次回加工id int,
稼动率  float, --每小时更新一次，0.50 表示50%
次回预定段取 nvarchar	(100),--
本日进度 float, --0.12 表示12%
进度快慢  nvarchar	(2) --较慢或正常
) 	
