
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 场地预约
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/changdiYuyue")
public class ChangdiYuyueController {
    private static final Logger logger = LoggerFactory.getLogger(ChangdiYuyueController.class);

    private static final String TABLE_NAME = "changdiYuyue";

    @Autowired
    private ChangdiYuyueService changdiYuyueService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private ChangdiService changdiService;//场地
    @Autowired
    private ChangdiCollectionService changdiCollectionService;//场地收藏
    @Autowired
    private ChangdiLiuyanService changdiLiuyanService;//场地留言
    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private JiaolianService jiaolianService;//教练
    @Autowired
    private NewsService newsService;//足球资讯
    @Autowired
    private QiuduiService qiuduiService;//球队
    @Autowired
    private QiuduiCollectionService qiuduiCollectionService;//球队收藏
    @Autowired
    private QiuduiLiuyanService qiuduiLiuyanService;//球队留言
    @Autowired
    private XunliandakaService xunliandakaService;//训练打卡
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("教练".equals(role))
            params.put("jiaolianId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = changdiYuyueService.queryPage(params);

        //字典表数据转换
        List<ChangdiYuyueView> list =(List<ChangdiYuyueView>)page.getList();
        for(ChangdiYuyueView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ChangdiYuyueEntity changdiYuyue = changdiYuyueService.selectById(id);
        if(changdiYuyue !=null){
            //entity转view
            ChangdiYuyueView view = new ChangdiYuyueView();
            BeanUtils.copyProperties( changdiYuyue , view );//把实体数据重构到view中
            //级联表 场地
            //级联表
            ChangdiEntity changdi = changdiService.selectById(changdiYuyue.getChangdiId());
            if(changdi != null){
            BeanUtils.copyProperties( changdi , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setChangdiId(changdi.getId());
            }
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(changdiYuyue.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ChangdiYuyueEntity changdiYuyue, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,changdiYuyue:{}",this.getClass().getName(),changdiYuyue.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            changdiYuyue.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<ChangdiYuyueEntity> queryWrapper = new EntityWrapper<ChangdiYuyueEntity>()
            .eq("changdi_id", changdiYuyue.getChangdiId())
            .eq("yonghu_id", changdiYuyue.getYonghuId())
            .eq("changdi_yuyue_time", new SimpleDateFormat("yyyy-MM-dd").format(changdiYuyue.getChangdiYuyueTime()))
            .in("changdi_yuyue_yesno_types", new Integer[]{1,2})
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ChangdiYuyueEntity changdiYuyueEntity = changdiYuyueService.selectOne(queryWrapper);
        if(changdiYuyueEntity==null){
            changdiYuyue.setInsertTime(new Date());
            changdiYuyue.setChangdiYuyueYesnoTypes(1);
            changdiYuyue.setCreateTime(new Date());
            changdiYuyueService.insert(changdiYuyue);
            return R.ok();
        }else {
            if(changdiYuyueEntity.getChangdiYuyueYesnoTypes()==1)
                return R.error(511,"有相同的待审核的数据");
            else if(changdiYuyueEntity.getChangdiYuyueYesnoTypes()==2)
                return R.error(511,"有相同的审核通过的数据");
            else
                return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ChangdiYuyueEntity changdiYuyue, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,changdiYuyue:{}",this.getClass().getName(),changdiYuyue.toString());
        ChangdiYuyueEntity oldChangdiYuyueEntity = changdiYuyueService.selectById(changdiYuyue.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            changdiYuyue.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

            changdiYuyueService.updateById(changdiYuyue);//根据id更新
            return R.ok();
    }


    /**
    * 审核
    */
    @RequestMapping("/shenhe")
    public R shenhe(@RequestBody ChangdiYuyueEntity changdiYuyueEntity, HttpServletRequest request){
        logger.debug("shenhe方法:,,Controller:{},,changdiYuyueEntity:{}",this.getClass().getName(),changdiYuyueEntity.toString());

        ChangdiYuyueEntity oldChangdiYuyue = changdiYuyueService.selectById(changdiYuyueEntity.getId());//查询原先数据

        if(changdiYuyueEntity.getChangdiYuyueYesnoTypes() == 2){//通过

            Wrapper<ChangdiYuyueEntity> queryWrapper = new EntityWrapper<ChangdiYuyueEntity>()
                    .eq("changdi_id", oldChangdiYuyue.getChangdiId())
                    .eq("changdi_yuyue_time", new SimpleDateFormat("yyyy-MM-dd").format(oldChangdiYuyue.getChangdiYuyueTime()))
                    .eq("changdi_yuyue_yesno_types", 2)
                    ;
            logger.info("sql语句:"+queryWrapper.getSqlSegment());
            ChangdiYuyueEntity changdiYuyueEntity1 = changdiYuyueService.selectOne(queryWrapper);
            if(changdiYuyueEntity1!=null){
                return R.error("该场地该天已有人通过预约,无法通过此次预约");
            }
        }else if(changdiYuyueEntity.getChangdiYuyueYesnoTypes() == 3){//拒绝
        }
        changdiYuyueEntity.setChangdiYuyueShenheTime(new Date());//审核时间
        changdiYuyueService.updateById(changdiYuyueEntity);//审核

        return R.ok();
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<ChangdiYuyueEntity> oldChangdiYuyueList =changdiYuyueService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        changdiYuyueService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<ChangdiYuyueEntity> changdiYuyueList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            ChangdiYuyueEntity changdiYuyueEntity = new ChangdiYuyueEntity();
//                            changdiYuyueEntity.setChangdiYuyueUuidNumber(data.get(0));                    //预约编号 要改的
//                            changdiYuyueEntity.setChangdiId(Integer.valueOf(data.get(0)));   //场地 要改的
//                            changdiYuyueEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            changdiYuyueEntity.setChangdiYuyueText(data.get(0));                    //预约理由 要改的
//                            changdiYuyueEntity.setChangdiYuyueTime(sdf.parse(data.get(0)));          //预约日期 要改的
//                            changdiYuyueEntity.setInsertTime(date);//时间
//                            changdiYuyueEntity.setChangdiYuyueYesnoTypes(Integer.valueOf(data.get(0)));   //预约状态 要改的
//                            changdiYuyueEntity.setChangdiYuyueYesnoText(data.get(0));                    //审核回复 要改的
//                            changdiYuyueEntity.setChangdiYuyueShenheTime(sdf.parse(data.get(0)));          //审核时间 要改的
//                            changdiYuyueEntity.setCreateTime(date);//时间
                            changdiYuyueList.add(changdiYuyueEntity);


                            //把要查询是否重复的字段放入map中
                                //预约编号
                                if(seachFields.containsKey("changdiYuyueUuidNumber")){
                                    List<String> changdiYuyueUuidNumber = seachFields.get("changdiYuyueUuidNumber");
                                    changdiYuyueUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> changdiYuyueUuidNumber = new ArrayList<>();
                                    changdiYuyueUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("changdiYuyueUuidNumber",changdiYuyueUuidNumber);
                                }
                        }

                        //查询是否重复
                         //预约编号
                        List<ChangdiYuyueEntity> changdiYuyueEntities_changdiYuyueUuidNumber = changdiYuyueService.selectList(new EntityWrapper<ChangdiYuyueEntity>().in("changdi_yuyue_uuid_number", seachFields.get("changdiYuyueUuidNumber")));
                        if(changdiYuyueEntities_changdiYuyueUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(ChangdiYuyueEntity s:changdiYuyueEntities_changdiYuyueUuidNumber){
                                repeatFields.add(s.getChangdiYuyueUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [预约编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        changdiYuyueService.insertBatch(changdiYuyueList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = changdiYuyueService.queryPage(params);

        //字典表数据转换
        List<ChangdiYuyueView> list =(List<ChangdiYuyueView>)page.getList();
        for(ChangdiYuyueView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ChangdiYuyueEntity changdiYuyue = changdiYuyueService.selectById(id);
            if(changdiYuyue !=null){


                //entity转view
                ChangdiYuyueView view = new ChangdiYuyueView();
                BeanUtils.copyProperties( changdiYuyue , view );//把实体数据重构到view中

                //级联表
                    ChangdiEntity changdi = changdiService.selectById(changdiYuyue.getChangdiId());
                if(changdi != null){
                    BeanUtils.copyProperties( changdi , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setChangdiId(changdi.getId());
                }
                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(changdiYuyue.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody ChangdiYuyueEntity changdiYuyue, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,changdiYuyue:{}",this.getClass().getName(),changdiYuyue.toString());
        Wrapper<ChangdiYuyueEntity> queryWrapper = new EntityWrapper<ChangdiYuyueEntity>()
            .eq("changdi_id", changdiYuyue.getChangdiId())
            .eq("changdi_yuyue_time", new SimpleDateFormat("yyyy-MM-dd").format(changdiYuyue.getChangdiYuyueTime()))
            .eq("changdi_yuyue_yesno_types", 2)
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ChangdiYuyueEntity changdiYuyueEntity = changdiYuyueService.selectOne(queryWrapper);
        if(changdiYuyueEntity==null){
            changdiYuyue.setInsertTime(new Date());
            changdiYuyue.setChangdiYuyueYesnoTypes(1);
            changdiYuyue.setCreateTime(new Date());
        changdiYuyueService.insert(changdiYuyue);

            return R.ok();
        }else {
                return R.error(511,"该天已有通过的预约,请选择其他日期");
        }
    }

}

