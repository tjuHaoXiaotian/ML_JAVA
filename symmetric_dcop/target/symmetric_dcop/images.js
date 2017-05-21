/**
 * Created by jack on 2017/5/3.
 */

var Config = {
    contrast_experiment:6
};

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
            var padding = {left:40, right:30, top:20, bottom:20};
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
                // console.log(root);


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
                    .gravity(0.2)
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
                        // console.log(d3.event);
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
                        //return d.id + ":" + d.centrality;
                        return d.id;
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
                    // console.log(data);
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

            var rec,text,times = 0,timer,timer2,key,stopTimes = 0,stop = false;
            stop = false;

            var index = [0, 1, 2, 3, 4,5,6,7,8,9];
            var color = ["#0310FF", "#D605FF", "#FF1A1F", "#FF6F12", "#FFEC04","#93FF16","#0BFFF1","#0310FF","#D605FF","#FF1A1F"];
            var ordinal = d3.scale.ordinal()
                .domain(index)
                .range(color);

            //定义比例尺

            var xScale = d3.scale.ordinal()
                .domain(d3.range(30))
                .rangeRoundBands([0, width - padding.left - padding.right]);

            var yScale = d3.scale.ordinal()
                .domain(d3.range(13))
                .rangeRoundBands([height - padding.top - padding.bottom, 0]);

            var sheets = [],sheet;

            //定义x轴
            var xAxis = d3.svg.axis()
                .scale(xScale)      //指定比例尺
                .orient("bottom")   //指定刻度的方向
                .ticks(30);          //指定刻度的数量
            //定义y轴
            var yAxis = d3.svg.axis()
                .scale(yScale)
                .orient("left");

            for(var expId = 0; expId < Config.contrast_experiment;expId++){
                sheet = d3.select(".item2-"+expId)     //选择文档中的body元素
                    .append("svg")          //添加一个svg元素
                    .attr("width", width)       //设定宽度
                    .attr("height", height);    //设定高度
                sheet.append("g")
                    .attr("class","axis")
                    .attr("transform","translate(" + padding.left + "," + (height - padding.bottom) + ")")
                    .call(xAxis);

                sheet.append("g")
                    .attr("class","axis")
                    .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                    .call(yAxis);

                sheets[expId+""] = sheet;
            }

            function drawActionSelection(root){
                var rectPadding = 2;
                for(key in root){
                    for(var key2 in root[key]) {
                        //console.log(key);
                        //console.log(key2);
                        //console.log(root[key]);
                        //console.log(sheets);
                        rec = sheets[key].append("rect")
                            .attr("class","MyRect")
                            .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                            .attr("x", xScale(times)+rectPadding/2)
                            .attr("y",yScale(key2))
                            .attr("width", xScale.rangeBand() - rectPadding)
                            .attr("height", yScale.rangeBand())
                            .style("fill",ordinal(key2));

                        //添加文字元素
                        text = sheets[key]
                            .append("text")
                            .attr("class","MyText")
                            .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                            .attr("x", xScale(times))
                            .attr("y",yScale(key2))
                            .attr("dx",function(){
                                //                    return (xScale.rangeBand())/2;
                                return 0;
                            })
                            .attr("dy",function(d){
                                return (yScale.rangeBand())/2;
                            })
                            .text(root[key][key2]);
                    }
                }
                if(stop){
                    clearInterval(timer)
                }
                times++;
                if(times >30){
                    for(var i = 0;i < Config.contrast_experiment;i++){
                        //alert("remove")
                        sheets[i+""].selectAll("rect.MyRect").remove();
                        sheets[i+""].selectAll("text.MyText").remove();
                    }
                    times = 0;
                }
            }

            timer = setInterval(function(){
                d3.json(url+type+"/agents/rowActions",function(error,root) {
                    if (error) {
                        return console.log(error);
                    }
//                console.log(root);
                    drawActionSelection(root)
                });

            },5000);

            timer2 = setInterval(function(){

                $.ajax({
                    url: url+type+"/stop",
                    type: 'get',
                    dataType: 'json'
                }).success(function(data){
                    stop = data.status;
                    if(stop){
                        drawSide(type);
                        drawRectangleSheet(type);
                        clearInterval(timer2);
                    }else{
                        drawSide(type);
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
                    var maxX = 0,tempMax = 0;
                    for(expId = 0; expId < Config.contrast_experiment;expId++){
                        tempMax = d3.max(data[expId],function (d) {
                            return d.round;
                        });
                        maxX = tempMax > maxX ? tempMax:maxX;
                    }

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

                    for(expId = 0; expId < Config.contrast_experiment; expId++){
                        svgSide.append("path")
                            .datum(data[expId])
                            .attr("class", "line-"+expId)
                            .attr("d", line)
                            .attr("transform","translate(" + padding.left + "," + padding.top + ")");
                    }

                });
            }

            function drawRectangleSheet(type) {

                d3.select(".item4").select("svg").remove();

                var svgSide = d3.select(".item4")     //选择文档中的body元素
                    .append("svg")          //添加一个svg元素
                    .attr("width", width)       //设定宽度
                    .attr("height", height);    //设定高度

                //x轴的比例尺
                var xScale = d3.scale.ordinal()
                    .domain(d3.range(Config.contrast_experiment))
                    .rangeRoundBands([0, width - padding.left - padding.right]);

//y轴的比例尺
                var yScale = d3.scale.linear()
                    .range([height - padding.top - padding.bottom, 0]);

                var xAxis = d3.svg.axis()
                    .scale(xScale)
                    .orient("bottom");

                var yAxis = d3.svg.axis()
                    .scale(yScale)
                    .orient("left");

                d3.json(url+ type +"/communications", function (error, data) {
                    if (error) throw error;

                    var rectPadding = 100;
                    // console.log(data);
                    var datas = [];
                    for(var key in data){
                        datas[key] = data[key];
                    }

                    yScale.domain([0,d3.max(datas) / 1000 * 2]);

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
                        .text("Messages sent times x1000");

                    //添加矩形元素
                    var rects = svgSide.selectAll(".MyRect")
                        .data(datas)
                        .enter()
                        .append("rect")
                        .attr("class",function(d,i){
                            return "rect-"+i;
                        })
                        .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                        .attr("x", function(d,i){
                            return xScale(i) + rectPadding/2;
                        } )
                        .attr("y",function(d){
                            // console.log(d);
                            return yScale(d/1000);
                        })
                        .attr("width", xScale.rangeBand() - rectPadding )
                        .attr("height", function(d){
                            return height - padding.top - padding.bottom - yScale(d/1000);
                        });

//添加文字元素
                    var texts = svgSide.selectAll(".MyText")
                        .data(datas)
                        .enter()
                        .append("text")
                        .attr("class","MyText")
                        .attr("transform","translate(" + padding.left + "," + padding.top + ")")
                        .attr("x", function(d,i){
                            return xScale(i) + rectPadding/2 - 28;
                        } )
                        .attr("y",function(d){
                            return yScale(d/1000);
                        })
                        .attr("dx",function(){
                            return (xScale.rangeBand() - rectPadding)/2;
                        })
                        .attr("dy",function(d){
                            return 20;
                        })
                        .text(function(d){
                            return d/1000;
                        });

                });
            }
        }
    }
}