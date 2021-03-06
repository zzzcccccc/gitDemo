app.controller('searchController',function($scope,searchService){	
	//搜索
	$scope.searchMap={'keywords':'','category':'','brand':'','spec':{},'price':'','pageNum':1,'pageSize':20,'sorts':'','fileds':''};//搜索对象
	$scope.resultMap = {};
	$scope.search=function(){
		searchService.search( $scope.searchMap ).success(
			function(response){			
				$scope.resultMap=response;//搜索返回的结果
				buildPageLabel();//调用
			}
		);	
	}	
	
	$scope.searchSort = function(key,filed){
		$scope.searchMap.priceSort = key;
		$scope.searchMap.fileds = filed;
		$scope.search();
	}
	
	//根据页码查询
	$scope.queryByPage=function(pageNum){
		//页码验证
		if(pageNum<1 || pageNum>$scope.resultMap.totalPages){
			return;
		}		
		$scope.searchMap.pageNum=pageNum;			
		$scope.search();
	}
	
	//判断当前页为第一页
	$scope.isTopPage=function(){
		if($scope.searchMap.pageNo==1){
			return true;
		}else{
			return false;
		}
	}
	
	//判断当前页是否未最后一页
	$scope.isEndPage=function(){
		if(resultMap == null){
			return false;
		}
		if($scope.searchMap.pageNo==$scope.resultMap.totalPages){
			return true;
		}else{
			return false;
		}
	}


	
	//构建分页标签(totalPages为总页数)
	buildPageLabel=function(){
		$scope.pageLabel=[];//新增分页栏属性		
		var maxPageNo= $scope.resultMap.totalPages;//得到最后页码
		var firstPage=1;//开始页码
		var lastPage=maxPageNo;//截止页码	
		
		$scope.firstDot=true;//前面有点
		$scope.lastDot=true;//后边有点	

		
		if($scope.resultMap.totalPages> 5){  //如果总页数大于5页,显示部分页码		
			if($scope.searchMap.pageNum<=3){//如果当前页小于等于3
				lastPage=5; //前5页
				$scope.firstDot=false;//前面没点
			}else if( $scope.searchMap.pageNum>=lastPage-2  ){//如果当前页大于等于最大页码-2
				firstPage= maxPageNo-4;		 //后5页	
				$scope.lastDot=false;//后边没点
			}else{ //显示当前页为中心的5页
				firstPage=$scope.searchMap.pageNum-2;
				lastPage=$scope.searchMap.pageNum+2;			
			}
		}else{
			$scope.firstDot=false;//前面无点
			$scope.lastDot=false;//后边无点
		}	
		//循环产生页码标签				
		for(var i=firstPage;i<=lastPage;i++){
			$scope.pageLabel.push(i);				
		}		
	}

	
	//添加搜索项
	$scope.addSearchItem=function(key,value){
		if(key=='category' || key=='brand' || key=='price'){//如果点击的是分类或者是品牌
			$scope.searchMap[key]=value;
		}else{
			$scope.searchMap.spec[key]=value;
		}	
		$scope.search();//执行搜索
	}
	
	//移除复合搜索条件
	$scope.removeSearchItem=function(key){
		if(key=="category" ||  key=="brand" || key=='price'){//如果是分类或品牌
			$scope.searchMap[key]="";		
		}else{//否则是规格
			delete $scope.searchMap.spec[key];//移除此属性
		}	
		$scope.search();//执行搜索
	}


	
});
