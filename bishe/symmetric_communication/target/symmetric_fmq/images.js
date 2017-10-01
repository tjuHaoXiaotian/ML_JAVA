/**
 * Created by jack on 2017/5/3.
 */


function Obj(url,type){
    return {
        init:function(){
            $(document).bind("contextmenu",function(e){
                return false;
            });

            $(document).on("click",function(){
                $("#q-table").hide();
            });

            $("#reset").on("click",function(e){
                $.ajax({
                    url: url+"clear/"+type,
                    type: 'get',
                    dataType: 'json'
                }).success(function(data){
                    alert("success!");
                    location.reload();
                }).error(function(error){
                    alert("failed!")
                });
            });

            var width = 950;  //画布的宽度
            var height = 600;   //画布的高度
            //画布周边的空白
            var padding = {left:30, right:30, top:20, bottom:20};
            var delta = 50;
            var nodes_r = 10;
            var svg = d3.select(".item1")     //选择文档中的body元素
                .append("svg")          //添加一个svg元素
                .attr("width", width)       //设定宽度
                .attr("height", height);    //设定高度

            d3.json(url+type,function(error,root){
                if( error ){
                    return console.log(error);
                }
                console.log(root);


                //      其中 linkDistance 是结点间的距离，
                //      charge 是定义结点间是吸引（值为正）还是互斥（值为负），值越大力越强。
                //       linkStrength： 指定连接线的坚硬度，值的范围为[ 0 , 1 ]，值越大越坚硬。
                //       gravity：以 size 函数设定的中心产生重力，各顶点都会向中心运动，默认值为0.1。也可以设定为0，则没有重力的作用。
                var force = d3.layout.force()
                    .nodes(root.nodes)
                    .links(root.edges)
                    .size([width,height])
                    .linkDistance(200)
//                    .linkStrength(0.8)
                    .gravity(0.6)
                    .charge(-1000)
                    .start();

                // 添加连线
                var svg_edges = svg.selectAll("line")
                    .data(root.edges)
                    .enter()
                    .append("line")
                    .style("stroke","#aaa")
                    .style("stroke-width",1);

                var color = d3.scale.category20();

                //添加节点
                var svg_nodes = svg.selectAll("circle")
                    .data(root.nodes)
                    .enter()
                    .append("circle")
                    .attr("r",nodes_r)
                    .style("fill",function(d,i){
                        return color(i);
//                        return "#1F77B4";
                    })
                    .call(force.drag)  //使得节点能够拖动
                    .on("mousedown",function(d) {
                        console.log(d3.event);
                        var button = d3.event.buttons;
                        if (2 == button) { //右键为2
                            queryQ(d.id,d3.event.clientX,d3.event.clientY);
                        } else if (1 == button) { //左键为1

                        }
                    });

                //添加描述节点的文字
                var svg_texts = svg.selectAll("text")
                    .data(root.nodes)
                    .enter()
                    .append("text")
                    .style("fill", "black")
                    .attr("dx", 20)
                    .attr("dy", 8)
                    .text(function(d){
                        return d.id + ":" + d.centrality;
                    });


                force.on("tick", function(){ //对于每一个时间间隔

                    //限制结点的边界
                    root.nodes.forEach(function(d,i){
                        d.x = d.x - nodes_r < 0     ? nodes_r : d.x ;
                        d.x = d.x + nodes_r > width ? width - nodes_r : d.x ;
                        d.y = d.y - nodes_r < 0      ? nodes_r : d.y ;
                        d.y = d.y + nodes_r > height ? height - nodes_r : d.y ;
                    });

                    //更新连线坐标
                    svg_edges.attr("x1",function(d){ return d.source.x; })
                        .attr("y1",function(d){ return d.source.y; })
                        .attr("x2",function(d){ return d.target.x; })
                        .attr("y2",function(d){ return d.target.y; });

                    //更新节点坐标
                    svg_nodes.attr("cx",function(d){ return d.x; })
                        .attr("cy",function(d){ return d.y; });

                    //更新文字坐标
                    svg_texts.attr("x", function(d){ return d.x; })
                        .attr("y", function(d){ return d.y; });
                });



            });

            function queryQ(id,x,y){
                $.ajax({
                    url: url + type+"/agents/"+id+"?expId=0",
                    type: 'get',
                    dataType: 'json',
                    data:$("#main-search-form").serialize()
                }).success(function(data) {
                    console.log(data);
                    initQTable(data,x,y);
                }).error(function(){
                    alert("查询失败");
                });
            }

            function initQTable(qArray,x,y){

                var table = $("#q-table");
                table.show();
                var stateNum = 1;
                var actionNum = qArray.length;
                table.html("");
                table.append($('<tr> <td colspan="'+(actionNum+1)+'" class="header">Q TABLE</td> </tr>'));
                var tr = $("<tr></tr>");
                tr.append($('<td>S\\A</td>'));
                var i = 0,j = 0,line = [],maxValue = 0,maxIndex = 0;
                for(i = 0;i < actionNum;i++){
                    tr.append($("<td>a"+i+"</td>"));
                }
                table.append(tr);

                maxValue = qArray[0].fmq;
                maxIndex = 0;
                for(j = 1;j < actionNum;j++){
                    if(qArray[j].fmq > maxValue){
                        maxValue = qArray[j].fmq;
                        maxIndex = j;
                    }
                }


                tr = $("<tr></tr>");

                tr.append($("<td>s0</td>"));
                for(j = 0;j < actionNum;j++){
                    if(j == maxIndex){
                        tr.append($('<td id="'+i+"_"+j+'" style="color:red;">'+(qArray[j].fmq).toFixed(2)+'</td>'))
                    }else{
                        tr.append($('<td id="'+i+"_"+j+'">'+(qArray[j].fmq).toFixed(2)+'</td>'))
                    }
                }
                table.append(tr);


                table.css("top",y);
                table.css("left",x);
            }



            var sheet = d3.select(".item2")     //选择文档中的body元素
                .append("svg")          //添加一个svg元素
                .attr("width", width)       //设定宽度
                .attr("height", height);    //设定高度


            var index = [0, 1, 2, 3, 4,5,6,7,8,9];
            var color = ["#0310FF", "#D605FF", "#FF1A1F", "#FF6F12", "#FFEC04","#93FF16","#0BFFF1","#0310FF","#D605FF","#FF1A1F"];
            var ordinal = d3.scale.ordinal()
                .domain(index)
                .range(color);

            //定义比例尺

            var xScale = d3.scale.ordinal()
                .domain(d3.range(50))
                .rangeRoundBands([0, width - padding.left - padding.right]);

            var yScale = d3.scale.ordinal()
                .domain(d3.range(10))
                .rangeRoundBands([height - padding.top - padding.bottom, 0]);
            //定义x轴
            var xAxis = d3.svg.axis()
                .scale(xScale)      //指定比例尺
                .orient("bottom")   //指定刻度的方向
                .ticks(100);          //指定刻度的数量

            //定义y轴
            var yAxis = d3.svg.axis()
                .scale(yScale)
                .orient("left");

            sheet.append("g")
                .attr("class","axis")
                .attr("transform","translate(" + padding.left + "," + (height - padding.bottom) + ")")
                .call(xAxis);

            sheet.append("g")
                .attr("class","axis")
                .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                .call(yAxis);

            var rec,text,times = 0,timer,timer2,key,count = 0,stopTimes = 0,stop = false;

            timer = setInterval(function(){
                d3.json(url+type+"/agents/rowActions?expId=0",function(error,root) {
                    if (error) {
                        return console.log(error);
                    }
//                console.log(root);

                    count = 0;
                    for(key in root) {
                        rec = sheet.append("rect")
                            .attr("class","MyRect")
                            .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                            .attr("x", xScale(times))
                            .attr("y",yScale(key))
                            .attr("width", xScale.rangeBand())
                            .attr("height", yScale.rangeBand())
                            .style("fill",ordinal(key));

                        //添加文字元素
                        text = sheet
                            .append("text")
                            .attr("class","MyText")
                            .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                            .attr("x", xScale(times))
                            .attr("y",yScale(key))
                            .attr("dx",function(){
                                //                    return (xScale.rangeBand())/2;
                                return 0;
                            })
                            .attr("dy",function(d){
                                return (yScale.rangeBand())/2;
                            })
                            .text(root[key]);
                        count++;
                    }
                    if(count == 1){
                        stopTimes++;
                        if(stopTimes >= 2) {
                            clearInterval(timer)
                        }
                    }

                });

                times++;

                if(times >50){
                    sheet.select(".MyRect").remove();
                    sheet.select(".MyText").remove();
                    times = 0;
                }

            },5000);



//        drawSide(0);
            timer2 = setInterval(function(){
                stop = false;
                $.ajax({
                    url: url+type+"/stop",
                    type: 'get',
                    dataType: 'json'
                }).success(function(data){
                    stop = data.status;
                    if(stop){
                        clearInterval(timer2)
                    }else{
                        drawSide(type)
                    }

                }).error(function(error){
                    alert(error.toString)
                });

            },2000);




            function drawSide(type) {

                d3.select(".item3").select("svg").remove();

                var svgSide = d3.select(".item3")     //选择文档中的body元素
                    .append("svg")          //添加一个svg元素
                    .attr("width", width)       //设定宽度
                    .attr("height", height);    //设定高度

                var x = d3.scale.linear()
                    .range([0, width - padding.left - padding.right]);

                var y = d3.scale.linear()
                    .range([height - padding.top - padding.bottom, 0]);

                var xAxis = d3.svg.axis()
                    .scale(x)
                    .orient("bottom");

                var yAxis = d3.svg.axis()
                    .scale(y)
                    .orient("left");

                var line = d3.svg.line()
                    .x(function(d) { return x(d.round); })
                    .y(function(d) { return y(d.reward); });
//                    .attr("transform","translate(" + padding.left + "," + padding.top + ")")

                d3.json(url+ type +"/avgPayoffs", function (error, data) {
                    if (error) throw error;

                    var expId = 0;
//                x.domain(d3.extent(data, function (d) {
//                    console.log("数据", d);
//                    return d.round;
//                }));
                    var maxX = 0;
                    maxX = d3.max(data[0],function (d) {
//                    console.log("数据", d);
                        return d.round;
                    }) > maxX?d3.max(data[0],function (d) {
//                    console.log("数据", d);
                        return d.round;
                    }):maxX;
                    maxX = d3.max(data[1],function (d) {
//                    console.log("数据", d);
                        return d.round;
                    }) > maxX?d3.max(data[1],function (d) {
//                    console.log("数据", d);
                        return d.round;
                    }):maxX;
                    maxX = d3.max(data[2],function (d) {
//                    console.log("数据", d);
                        return d.round;
                    }) > maxX?d3.max(data[2],function (d) {
//                    console.log("数据", d);
                        return d.round;
                    }):maxX;
                    x.domain([0,maxX]);

                    y.domain([-1,1]);

                    svgSide.append("g")
                        .attr("class", "x axis")
                        .attr("transform","translate(" + padding.left + "," + (height - padding.bottom) + ")")
                        .call(xAxis);

                    svgSide.append("g")
                        .attr("class", "y axis")
                        .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                        .call(yAxis)
                        .append("text")
                        .attr("transform", "rotate(-90)")
                        .attr("y", 6)
                        .attr("dy", ".71em")
                        .style("text-anchor", "end")
                        .text("平均 payoff");

                    for(expId = 0; expId < 3; expId++){
                        svgSide.append("path")
                            .datum(data[expId])
                            .attr("class", "line-"+expId)
                            .attr("d", line)
                            .attr("transform","translate(" + padding.left + "," + padding.top + ")");
                    }

                });
            }

        }
    }
}