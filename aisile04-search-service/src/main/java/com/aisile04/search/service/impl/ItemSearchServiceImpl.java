package com.aisile04.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.scheduling.annotation.Scheduled;

import com.aisile04.pojo.TbItem;
import com.aisile04.search.service.ItemSearchService;
import com.aisile04.solrutil.service.SolrUtil;
import com.alibaba.dubbo.config.annotation.Service;
@Service
public class ItemSearchServiceImpl implements ItemSearchService{

	@Autowired
	SolrTemplate solrTemplate;
	
	@Autowired
	private RedisTemplate redisTemplate;

	
	/**
	 * 查询分类列表    分组查询关键字
	 * @param searchMapsearchCategoryList
	 * @return
	 */
	private  List searchCategoryList(Map searchMap){
		List<String> list=new ArrayList();	
		Query query=new SimpleQuery();		
		//按照关键字查询
		Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		//设置分组选项
		//根据item_category进行分组，指定分组的字段，相当于（group by item_category）
		GroupOptions groupOptions=new GroupOptions().addGroupByField("item_category");
		query.setGroupOptions(groupOptions);
	
		//得到分组页，开启分组
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
		//根据列得到分组结果集
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		//得到分组结果入口页
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		//得到分组入口集合
		List<GroupEntry<TbItem>> content = groupEntries.getContent();
		for(GroupEntry<TbItem> entry:content){
			list.add(entry.getGroupValue());//将分组结果的名称封装到返回值中	
		}
		return list;
	}

	
	
	@Override
	public Map<String, Object> search(Map searchMap) {
		System.out.println("1111");
		if(searchMap.get("keywords")==null || "".equals(searchMap.get("keywords"))){
			return null;
		}
		
		
		// TODO Auto-generated method stub
		Map<String,Object> map=new HashMap<>();
		//调用分类结果
		List categoryList = searchCategoryList(searchMap);
		map.put("cateList", searchCategoryList(searchMap));
		
		map.putAll(searchList(searchMap));
		//添加品牌规格
		if(searchCategoryList(searchMap)!= null && searchCategoryList(searchMap).size()>0){
			map.putAll(searchBrandAndSpec((String)searchCategoryList(searchMap).get(0)));
		}
		map.put("categoryList",categoryList);		
		//3.查询品牌和规格列表
		String categoryName=(String)searchMap.get("category");
		if(!"".equals(categoryName)){//如果有分类名称
			map.putAll(searchBrandAndSpec(categoryName));			
		}else{//如果没有分类名称，按照第一个查询
			if(categoryList.size()>0){
				map.putAll(searchBrandAndSpec((String)categoryList.get(0)));
			}
		}
		return map;
	}
	
	/**
	 * 查询品牌和规格列表
	 * @param category 分类名称
	 * @return
	 */
	private Map searchBrandAndSpec(String typeName){
		Map map=new HashMap();	
		/*Object itemCatWords = searchMap.get("itemCat");
		if(itemCatWords==null){
			return null;
		}*/
		Long typeId = (Long) redisTemplate.boundHashOps("itemCatList").get(typeName);//获取模板ID
		if(typeId!=null){
			//根据模板ID查询品牌列表 
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
			map.put("brandList", brandList);//返回值添加品牌列表
			//根据模板ID查询规格列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
			map.put("specList", specList);				
		}			
		return map;
	}

	
	
	/**
	 * 根据关键字高量显示搜索列表
	 * @param keywords
	 * @return
	 */
	private Map searchList(Map searchMap){
		Map map=new HashMap();
		//前端传递过来的对象
		String keyWords = (String)searchMap.get("keywords");
		
		//关键字空格处理 
		keyWords = keyWords.replaceAll(" ", "");

		
		//开启高量显示查询   设置高量显示的域
		HighlightQuery query=new SimpleHighlightQuery();
		HighlightOptions highlightOptions=new HighlightOptions().addField("item_title","item_seller");//设置高亮的域
		//设置前缀后缀
		highlightOptions.setSimplePrefix("<em style='color:red'>");//高亮前缀 
		highlightOptions.setSimplePostfix("</em>");//高亮后缀
		//设置高亮选项
		query.setHighlightOptions(highlightOptions);
		Criteria criteria=new Criteria("item_keywords").is(keyWords);
		query.addCriteria(criteria);
		
		
		//1.2按分类筛选
		if(!"".equals(searchMap.get("category"))){		
			Criteria filterCriteria=new Criteria("item_category").is(searchMap.get("category"));
			FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
			query.addFilterQuery(filterQuery);
		}
		//1.3按品牌筛选
		if(!"".equals(searchMap.get("brand"))){	
			Criteria filterCriteria=new Criteria("item_brand").is(searchMap.get("brand"));
			FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
			query.addFilterQuery(filterQuery);
		}
		//1.4过滤规格
		if(searchMap.get("spec")!=null){
				Map<String,String> specMap= (Map) searchMap.get("spec");
				for(String key:specMap.keySet() ){
					Criteria filterCriteria=new Criteria("item_spec_"+key).is( specMap.get(key) );
					FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
					query.addFilterQuery(filterQuery);				
				}			
		}
		
		//1.7排序
		if(searchMap.get("sorts")!=null && !"".equals(searchMap.get("fileds"))){  
			Sort sort = null;
			if(("ASC").equals(searchMap.get("sorts"))){
				sort=new Sort(Sort.Direction.ASC, "item_"+searchMap.get("fileds"));
			}
			if(("DESC").equals(searchMap.get("sorts"))){
				sort=new Sort(Sort.Direction.DESC, "item_"+searchMap.get("fileds"));
			}
			query.addSort(sort);
		}

		
		//价格区间查询
		if(!"".equals(searchMap.get("price"))){
			String[] price = ((String) searchMap.get("price")).split("-");
			if(!price[0].equals("0")){//如果区间起点不等于0
				Criteria filterCriteria=new Criteria("item_price").greaterThanEqual(price[0]);
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);				
			}		
			if(!price[1].equals("*")){//如果区间终点不等于*
				Criteria filterCriteria=new  Criteria("item_price").lessThanEqual(price[1]);
				FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);				
			}
		}
		
		//设置初始页码
		Integer pageNum= (Integer) searchMap.get("pageNum");
		if(pageNum==null){
			pageNum = 1;
		}
		
		Integer pageSize=(Integer) searchMap.get("pageSize");//每页记录数 
		if(pageSize==null){
			pageSize=20;//默认20
		}
		//设置初始页面显示数
		query.setOffset((pageNum-1)*pageSize);//从第几条记录查询
		query.setRows(pageSize);

		
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		//只是得到普通集合
		//page.getContent();
		List<HighlightEntry<TbItem>> list = page.getHighlighted();
		for (HighlightEntry<TbItem> highlightEntry : list) {
			if(highlightEntry.getHighlights().size()>0 && highlightEntry.getHighlights().size()>0){
				//查到值
				//有值才可以循环  for循环特性
				for (Highlight entry : highlightEntry.getHighlights()) {
					if("item_title".equals(entry.getField().getName())){
						highlightEntry.getEntity().setTitle(entry.getSnipplets().get(0));
					}
					if("item_seller".equals(entry.getField().getName())){
						highlightEntry.getEntity().setSeller(entry.getSnipplets().get(0));
					}
				}
			}
		}
		map.put("rows",page.getContent());//分页后的数据
		map.put("totalPages", page.getTotalPages());//总页数
		map.put("total", page.getTotalElements());//总记录书
		
		return map;
	}



	//同步solr库
	@Override
	public void importList(List<TbItem> list) {
		solrTemplate.saveBeans(list);	
		solrTemplate.commit();
	}
	//任务调度
	@Scheduled(cron="0 40 9 * * ?")
	public void import1() {
		SolrUtil.importSorl();
		System.out.println("1");
	}

	@Override
	public void deleteSolrByGoodsId(String id) {
		// TODO Auto-generated method stub
		Query query = new SimpleQuery("item_goodsid:"+id);
		solrTemplate.delete(query);
		solrTemplate.commit();
		System.out.println("删除solr索引库");
	}
	
	

}
