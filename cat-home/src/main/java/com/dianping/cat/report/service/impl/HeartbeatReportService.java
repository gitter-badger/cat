package com.dianping.cat.report.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.unidal.dal.jdbc.DalException;

import com.dianping.cat.Cat;
import com.dianping.cat.consumer.heartbeat.HeartbeatReportMerger;
import com.dianping.cat.consumer.heartbeat.model.entity.HeartbeatReport;
import com.dianping.cat.core.dal.HourlyReport;
import com.dianping.cat.core.dal.HourlyReportEntity;
import com.dianping.cat.helper.TimeUtil;
import com.dianping.cat.message.Message;
import com.dianping.cat.report.service.AbstractReportService;

public class HeartbeatReportService extends AbstractReportService<HeartbeatReport> {

	@Override
	public HeartbeatReport makeReport(String domain, Date start, Date end) {
		HeartbeatReport report = new HeartbeatReport(domain);

		report.setStartTime(start);
		report.setEndTime(end);
		return report;
	}

	@Override
	public HeartbeatReport queryDailyReport(String domain, Date start, Date end) {
		throw new RuntimeException("Heartbeat report don't support daily report");
	}

	@Override
	public HeartbeatReport queryHourlyReport(String domain, Date start, Date end) {
		HeartbeatReportMerger merger = new HeartbeatReportMerger(new HeartbeatReport(domain));
		long startTime = start.getTime();
		long endTime = end.getTime();
		String name = "heartbeat";

		for (; startTime < endTime; startTime = startTime + TimeUtil.ONE_HOUR) {
			List<HourlyReport> reports = null;
			try {
				reports = m_hourlyReportDao.findAllByDomainNamePeriod(new Date(startTime), domain, name,
				      HourlyReportEntity.READSET_FULL);
			} catch (DalException e) {
				Cat.logError(e);
			}
			if (reports != null) {
				for (HourlyReport report : reports) {
					String xml = report.getContent();

					try {
						HeartbeatReport reportModel = com.dianping.cat.consumer.heartbeat.model.transform.DefaultSaxParser
						      .parse(xml);
						reportModel.accept(merger);
					} catch (Exception e) {
						Cat.logError(e);
						Cat.getProducer().logEvent("ErrorXML", name, Message.SUCCESS,
						      report.getDomain() + " " + report.getPeriod() + " " + report.getId());
					}
				}
			}
		}
		HeartbeatReport heartbeatReport = merger.getHeartbeatReport();

		heartbeatReport.setStartTime(start);
		heartbeatReport.setEndTime(new Date(end.getTime() - 1));

		Set<String> domains = queryAllDomainNames(start, end, "heartbeat");
		heartbeatReport.getDomainNames().addAll(domains);
		return heartbeatReport;
	}

	@Override
	public HeartbeatReport queryMonthlyReport(String domain, Date start) {
		throw new RuntimeException("Heartbeat report don't support monthly report");
	}

	@Override
	public HeartbeatReport queryWeeklyReport(String domain, Date start) {
		throw new RuntimeException("Heartbeat report don't support weekly report");
	}

}