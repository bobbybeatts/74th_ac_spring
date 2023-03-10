package kr.co.seoulit.account.sys.base.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import kr.co.seoulit.account.operate.system.to.PeriodBean;
import kr.co.seoulit.account.posting.business.to.SlipBean;
import kr.co.seoulit.account.sys.base.mapper.*;
import kr.co.seoulit.account.sys.base.to.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.seoulit.account.operate.system.mapper.AuthorityGroupMapper;
import kr.co.seoulit.account.operate.system.to.AuthorityEmpBean;
import kr.co.seoulit.account.sys.base.exception.DeptCodeNotFoundException;
import kr.co.seoulit.account.sys.base.exception.IdNotFoundException;
import kr.co.seoulit.account.sys.base.exception.PwMissmatchException;
import kr.co.seoulit.account.sys.common.exception.DataAccessException;
import kr.co.seoulit.account.sys.common.sl.ServiceLocator;
import lombok.extern.slf4j.Slf4j;
import kr.co.seoulit.account.operate.humanresource.mapper.EmployeeMapper;
import kr.co.seoulit.account.operate.humanresource.to.EmployeeBean;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.query.JRQueryExecuterFactory;
import net.sf.jasperreports.engine.query.QueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRProperties;
import oracle.jdbc.OracleTypes;

@Slf4j
@Service
@Transactional
public class BaseServiceImpl implements BaseService {

	@Autowired
	private PeriodNoMapper periodNoMapper;
	@Autowired
	private MenuMapper menuDAO;
	@Autowired
	private EmployeeMapper employeeDAO;
	@Autowired
	private PeriodMapper periodDAO;
	@Autowired
	private CodeMapper codeDAO;
	@Autowired
	private DetailCodeMapper detailCodeDAO;
	@Autowired
	private AuthorityGroupMapper authorityDAO;
	@Autowired
	private DataSource dataSource;
	@Autowired
	private BoardMapper boardDAO;

	@Autowired
	private MonthMapper monthMapper;


	@Override
	public HashMap<String, String> findUrlMapper() {

		HashMap<String, String> map = new HashMap<>();

		for(MenuBean menubean: menuDAO.selectAllMenuList()) {
			map.put(menubean.getMenuCode(), menubean.getUrl());
		}

		return map;
	}

	public void modifyEarlyStatements(String periodNo) {


		periodDAO.updateEarlyStatements(periodNo);

	}


	public String findPeriodNo(String today) {

		String periodNo=null;
		periodNo = periodDAO.getPeriodNo(today);

		return periodNo;
	}


	public void registerPeriodNo(String sdate,String edate) {

		periodDAO.insertPeriodNo(sdate,edate);

	}

	//????????? ????????????

	public ArrayList<IreportBean> findIreportData(HttpServletRequest request, HttpServletResponse response,
												  String slipNo) {

		ArrayList<IreportBean> reportDataList = null;
		HashMap<String, Object> parameters = new HashMap<>();
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("utf-8");
		System.out.println("      @ DB ?????? : getReportData");
		try {
			DataSource dataSource = ServiceLocator.getInstance().getDataSource("jdbc/ac2");
			Connection conn = dataSource.getConnection();

			parameters.put("slip_no", slipNo);

			String path = "/resources/reportform/report11.jasper";
			String rPath = request.getServletContext().getRealPath(path); //C?????? ????????????
			System.out.println(rPath); //C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\Account\resources\reportform\report11.jasper

			InputStream inputStream = new FileInputStream(rPath); //??????
			// ?????????????????? xml??? ?????? ?????????
			JasperReport jasperReport = (JasperReport) JRLoader.loadObject(inputStream);
			// ???????????? (???????????? jrml??????, ???????????? ???????????????_where???????????? ????????????????????? ????????? ?????????, db??????)
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conn);

			System.out.println("Ireport ??????1");

			/////////////reportDataList = baseCodeApplicationService.getIreportData(slipNo);
			ServletOutputStream out = response.getOutputStream(); //??????
			// ?????????????????? pdf?????? ????????? ????????????. ???????????? ????????? jsp??? ???????????? ???????????? ??????
			response.setContentType("application/pdf");

			JasperExportManager.exportReportToPdfStream(jasperPrint, out);
			JasperExportManager.exportReportToPdfFile(jasperPrint,
					"C:\\dev\\account\\pdf\\" + slipNo + ".pdf");

			// ?????????????????? ????????? ???????????????
			out.flush();
		}catch (Exception e) {
			System.out.println("  @????????????");
		}

		return null;
	}

	@Override
	public EmployeeBean findLoginData(String empCode, String userPw) throws IdNotFoundException, DeptCodeNotFoundException, PwMissmatchException {

		EmployeeBean employeeBean;

		try {
			employeeBean = employeeDAO.selectEmployee(empCode);

			if (employeeBean == null)
				throw new IdNotFoundException("?????? ?????? ?????? ???????????????.");
			else {
				if (!employeeBean.getUserPw().equals(userPw))
					throw new PwMissmatchException("??????????????? ????????????.");

			}
		} catch (DataAccessException e) {
			throw e;
		}
		return employeeBean;
	}

	@Override
	public ArrayList<MenuBean> findUserMenuList(String deptCode) {

		System.out.println("??????????????????");
		ArrayList<MenuBean> menuList = null;
		menuList = menuDAO.selectMenuNameList(deptCode);

		return menuList;
	}

	@Override
	public ArrayList<DetailCodeBean> findDetailCodeList(HashMap<String, String> param) {


		ArrayList<DetailCodeBean> datailCondeList = null;
		datailCondeList = detailCodeDAO.selectDetailCodeList(param);

		return datailCondeList;
	}

	@Override
	public ArrayList<CodeBean> findCodeList() {

		ArrayList<CodeBean> codeList = null;
		codeList = codeDAO.selectCodeList();

		return codeList;
	}

	@Override
	public void batchCodeProcess(ArrayList<CodeBean> codeList, ArrayList<DetailCodeBean> codeList2) {

		for (CodeBean code : codeList) {
			switch (code.getStatus()) {
				case "insert":
					codeDAO.insertCode(code);
					break;
				case "update":
					codeDAO.updateCode(code);
					break;
				case "normal":
					break;
				case "delete":
					codeDAO.deleteCode(code.getDivisionCodeNo());
			}
		}
		ArrayList<DetailCodeBean> DetailcodeList = codeList2;
		for (DetailCodeBean codeDetailBean : DetailcodeList) {
			switch (codeDetailBean.getStatus()) {
				case "insert":
					detailCodeDAO.insertDetailCode(codeDetailBean);
					break;
				case "update":
					detailCodeDAO.updateDetailCode(codeDetailBean);
					break;
				case "delete":
					detailCodeDAO.deleteDetailCode(codeDetailBean.getDetailCode());
			}
		}
	}

	public void findIreportTotalData(HttpServletRequest request, HttpServletResponse response) {


		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("utf-8");

		System.out.println("      @ DB ?????? : getReportData");
		try {

			Connection conn = dataSource.getConnection();

			String path = "http://localhost/ireport/totalTrialBalance.jrxml";

			URL url = new URL(path);
			URLConnection connection = url.openConnection();

			// ?????????????????? xml??? ?????? ?????????
			JasperReport jasperReport = JasperCompileManager.compileReport(connection.getInputStream());
			// ????????? ??????(*.jrxml)  ==> ???????????? ??????(*.jasper)
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,null,conn);
			//JasperFillManager??? jasperPrint ?????? ??????
			// (?????????, ????????????, connection)




			ServletOutputStream out = response.getOutputStream();
			response.setContentType("application/pdf");
			// html????????? ????????? ????????? ??????
			System.out.println("Ireport ??????2 :" +out);
			System.out.println("Ireport ??????3 :" +jasperPrint);

			JasperExportManager.exportReportToPdfStream(jasperPrint, out);

			out.flush();   // ?????? ??? OutputStream ??????(?????????)

			System.out.println("      @ DB ??????");
		} catch (Exception e) {

			System.out.println(e+ "      @ DB ??????");
		}

	}

	@Override
	public void findIreportData3(HttpServletRequest request, HttpServletResponse response) {
		// TODO Auto-generated method stub

		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("utf-8");
		HashMap<String, Object> parameters = new HashMap<>();
		System.out.println("      @ DB ?????? : getReportData");
		try {
			DataSource dataSource = ServiceLocator.getInstance().getDataSource("jdbc/ac2");
			Connection conn = dataSource.getConnection();

			String path = "/resources/ireport/financStatus.jrxml";
			String rPath = request.getServletContext().getRealPath(path);
			System.out.println(rPath);
			// ?????????????????? xml??? ?????? ?????????
			System.out.println(request.getParameter("from"));
			System.out.println(request.getParameter("to"));

			parameters.put("param_1", request.getParameter("from"));
			parameters.put("param_2", request.getParameter("to"));
			//parameters.put("param_3", OracleTypes.NUMBER);
			//parameters.put("param_4", OracleTypes.VARCHAR);
			parameters.put("ORACLE_REF_CURSOR", OracleTypes.CURSOR);

			JRProperties.setProperty(QueryExecuterFactory.QUERY_EXECUTER_FACTORY_PREFIX+"plsql"
					,"com.jaspersoft.jrx.query.PlSqlQueryExecuterFactory");
			JasperReportsContext jasperReportsContext = DefaultJasperReportsContext.getInstance();
			JRPropertiesUtil jrPropertiesUtil = JRPropertiesUtil.getInstance(jasperReportsContext);
			jrPropertiesUtil.setProperty("net.sf.jasperreports.query.executer.factory.plsql", "net.sf.jasperreports.engine.query.PlSqlQueryExecuterFactory");

			JasperReport jasperReport = JasperCompileManager.compileReport(rPath);


			jasperReport.setProperty(JRQueryExecuterFactory.QUERY_EXECUTER_FACTORY_PREFIX + "<query language>", "<value>");

			jasperReport.setProperty( "net.sf.jasperreports.query.executer.factory.plsql"
					,"com.jaspersoft.jrx.query.PlSqlQueryExecuterFactory");





			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,parameters,conn);

			System.out.println("Ireport ??????3");

			ServletOutputStream out = response.getOutputStream();
			response.setContentType("application/pdf");
			JasperExportManager.exportReportToPdfStream(jasperPrint, out);
			out.flush();

		} catch (Exception e) {
			System.out.println("      @ ????????????");
		}
	}

	@Override
	public void findIreportData4(HttpServletRequest request, HttpServletResponse response) {
		// TODO Auto-generated method stub

		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("utf-8");
		HashMap<String, Object> parameters = new HashMap<>();
		System.out.println("      @ DB ?????? : getReportData");
		try {
			DataSource dataSource = ServiceLocator.getInstance().getDataSource("jdbc/ac2");
			Connection conn = dataSource.getConnection();

			String path = "/resources/ireport/imTotalTrialBalance.jrxml";
			String rPath = request.getServletContext().getRealPath(path);
			System.out.println(rPath);
			// ?????????????????? xml??? ?????? ?????????
			System.out.println(request.getParameter("from"));
			System.out.println(request.getParameter("to"));

			parameters.put("param_1", request.getParameter("from"));
			parameters.put("param_2", request.getParameter("to"));
			//parameters.put("param_3", OracleTypes.NUMBER);
			//parameters.put("param_4", OracleTypes.VARCHAR);
			parameters.put("ORACLE_REF_CURSOR", OracleTypes.CURSOR);

			JRProperties.setProperty(QueryExecuterFactory.QUERY_EXECUTER_FACTORY_PREFIX+"plsql"
					,"com.jaspersoft.jrx.query.PlSqlQueryExecuterFactory");
			JasperReportsContext jasperReportsContext = DefaultJasperReportsContext.getInstance();
			JRPropertiesUtil jrPropertiesUtil = JRPropertiesUtil.getInstance(jasperReportsContext);
			jrPropertiesUtil.setProperty("net.sf.jasperreports.query.executer.factory.plsql", "net.sf.jasperreports.engine.query.PlSqlQueryExecuterFactory");

			JasperReport jasperReport = JasperCompileManager.compileReport(rPath);


			jasperReport.setProperty(JRQueryExecuterFactory.QUERY_EXECUTER_FACTORY_PREFIX + "<query language>", "<value>");

			jasperReport.setProperty( "net.sf.jasperreports.query.executer.factory.plsql"
					,"com.jaspersoft.jrx.query.PlSqlQueryExecuterFactory");





			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,parameters,conn);

			System.out.println("Ireport ??????4");
			ServletOutputStream out = response.getOutputStream();
			response.setContentType("application/pdf");
			JasperExportManager.exportReportToPdfStream(jasperPrint, out);
			out.flush();

		} catch (Exception e) {
			System.out.println("      @ ????????????");
		}
	}

	@Override
	public ArrayList<AuthorityEmpBean> findAuthority(String empCode) {

		ArrayList<AuthorityEmpBean> authorityEmp = null;
		authorityEmp = authorityDAO.selectAuthorityEmp(empCode);

		return authorityEmp;
	}

	@Override
	public void findIreportDataincome(HttpServletRequest request, HttpServletResponse response) {

		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("utf-8");
		System.out.println("      @ DB ?????? : getReportDataincome");
		try {
			Connection conn = dataSource.getConnection();

			String path = "https://account71.s3.ap-northeast-2.amazonaws.com/ireport/incomeStatementPdf.jrxml";

			URL url = new URL(path);
			URLConnection connection = url.openConnection();

			// ?????????????????? xml??? ?????? ?????????
			JasperReport jasperReport = JasperCompileManager.compileReport(connection.getInputStream());
			// ????????? ??????(*.jrxml)  ==> ???????????? ??????(*.jasper)
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,null,conn);
			//JasperFillManager??? jasperPrint ?????? ??????
			// (?????????, ????????????, connection)

			System.out.println("Ireportincome ??????");

			ServletOutputStream out = response.getOutputStream();
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "inline");
			// html????????? ????????? ????????? ??????
			JasperExportManager.exportReportToPdfStream(jasperPrint, out);
			out.flush();   // ?????? ??? OutputStream ??????(?????????)

		} catch (Exception e) {
			System.out.println("      @ ????????????");
		}
	}

	@Override
	public void findIreportDatafinance(HttpServletRequest request, HttpServletResponse response) {

		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("utf-8");
		System.out.println("      @ DB ?????? : getReportDataincome");
		try {

			Connection conn = dataSource.getConnection();

			String path = "http://localhost/ireport/financialPositionPdf.jrxml";

			URL url = new URL(path);
			URLConnection connection = url.openConnection();

			// ?????????????????? xml??? ?????? ?????????
			JasperReport jasperReport = JasperCompileManager.compileReport(connection.getInputStream());
			// ????????? ??????(*.jrxml)  ==> ???????????? ??????(*.jasper)
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,null,conn);
			//JasperFillManager??? jasperPrint ?????? ??????
			// (?????????, ????????????, connection)

			System.out.println("Ireportincome ??????");


			ServletOutputStream out = response.getOutputStream();
			response.setContentType("application/pdf");
			// html????????? ????????? ????????? ??????
			JasperExportManager.exportReportToPdfStream(jasperPrint, out);
			out.flush();   // ?????? ??? OutputStream ??????(?????????)

			System.out.println("      @ DB ??????");
		} catch (Exception e) {

			System.out.println("      @ DB ??????");
		}
	}

	@Override
	public ArrayList<BoardBean> findParentboardList() {
		System.out.println("????????? ?????? ??????@@@@@@@@@@");
		ArrayList<BoardBean> accountList = null;
		accountList = boardDAO.selectParentBoardList();
		return accountList;
	}

	@Override
	public ArrayList<BoardBean> findDetailboardList(String id) {
		ArrayList<BoardBean> accountList = null;
		accountList = boardDAO.selectDetailBoardList(id);
		return accountList;
	}
	@Override
	public ArrayList<BoardBean> findDetailboardList1(String id) {
		ArrayList<BoardBean> accountList = null;
		accountList = boardDAO.selectDetailBoardList1(id);
		return accountList;
	}

	@Override
	@Transactional
	public void deleteBoard(String id) {
		boardDAO.deleteBoardList(id);
		System.out.println("????????????????????????");

	}

	@Override
	public void updateLookup(String id) {
		boardDAO.updateLookup(id);

	}

	@Override
	public void insertBoard(BoardBean boardbean) throws Exception {

		boardDAO.insertBoard(boardbean);



	}

	@Override
	public void boardModify(BoardBean boardbean) {
		boardDAO.boardModify(boardbean);

	}

	@Override
	public ArrayList<BoardBean> showreply(String id) {
		// TODO Auto-generated method stub
		ArrayList<BoardBean> replyList = null;
		replyList = boardDAO.selectreplyList(id);
		System.out.println("?????? ?????????@@@@@@@@@@@@@@@@@");
		return replyList;
	}

	@Override
	public void insertReBoard(BoardBean boardbean) {


		boardDAO.insertReBoard(boardbean);

	}

	@Override
	public void deletereBoard(String rid) {
		// TODO Auto-generated method stub

		boardDAO.deleteReBoard(rid);

	}

	@Override
	public void boardReModify(BoardBean boardbean) {
		// TODO Auto-generated method stub
		boardDAO.modifyReBoard(boardbean);
	}

	@Override
	public void fileInsert(BoardBean boardBean) throws Exception{
		boardDAO.fileInsert(boardBean);
	}

	@Override
	public List<PeriodNoBean> findPeriodNo(){
		return periodNoMapper.findPeriodNo();
	}

	@Override
	public List<MonthBean> findMonth() {return monthMapper.findMonth();}

	@Override
	public PeriodNoBean findTPeriodNo(HashMap<String, Object> map) {
		// TODO Auto-generated method stub
		return periodNoMapper.selectPeriodNo(map);
	}
	
}

