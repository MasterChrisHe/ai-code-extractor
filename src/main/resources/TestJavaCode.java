package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    /**
     * 查询分页
     * 需要以1，10的倍数形式分页
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );
        if(page.isSearchCount()){
           //注释1 1234
            List<CategoryEntity> records = page.getRecords();
        }else{
            for(int i=0;i<5;i++){
               //注释2
            }
        }

        if(page.getRecords().size()>0){
               //注释3
            List<CategoryEntity> records = page.getRecords();
            if(records.size()==5){
                //注释4
                CategoryEntity categoryEntity = records.get(3);
            }
        }

        switch((int) page.getCurrent()){
            //switch注释
            case 1:
                //switch case 1 注释
                break;
            case 2:
                //switch case 2 注释
                break;
            default:
                //switch default注释
                break;
        }

        // 注释6
        if(page.getRecords().size()==0){
              //注释7
        }

        return new PageUtils(page);
    }

    // list查树
    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2、1 找到所有的一级分类
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter(a -> a.getParentCid() == 0).collect(Collectors.toList());

        //2、2所有的一级分类再设置子结构进去
        List<CategoryEntity> collect = level1Menus.stream().map(entity -> {
            entity.setChildren(getChildren(entity, categoryEntities));
            return entity;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public void removeMenuByIds(List<Long> list) {
        //todo 1、检查当前删除的菜单，是否被其他地方引用

        baseMapper.deleteBatchIds(list);
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> collect = all.stream().filter(entity -> Objects.equals(entity.getParentCid(), root.getCatId()))
                .map(entity -> {
                    entity.setChildren(getChildren(entity, all));
                    return entity;
                }).collect(Collectors.toList());
        return collect;
    }

    private void abc(){

    }

}