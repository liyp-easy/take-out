package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //构造ShoppingCart封装请求参数，因为它包含用户的id。
        ShoppingCart shoppingCart = new ShoppingCart();
        //对象属性拷贝：dishId、setmealId、dishFlavor
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //设置用户id：用户端每次发送请求都会携带token，通过JwtTokenUserInterceptor拦截器去解析
        //         这个token,解析出来的用户id和ThreadLocal进行绑定，之后在这个地方通过ThreadLocal取出来即可。
        Long userid = BaseContext.getCurrentId();
        shoppingCart.setUserId(userid);
        //1.判断当前加入到购物车中的商品是否已经存在
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList != null && shoppingCartList.size() == 1) {
            //2.如果已经存在，就更新数量，数量加1
            shoppingCart = shoppingCartList.get(0);//取出来第一条数据 也是唯一的一条数据。
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);//在原先的数量基础上加1，之后执行update语句。
            // update shopping_cart set number = ? where id = ?
            //因为是从数据库里面查出来的，所以cart里面一定是有id的。
            shoppingCartMapper.updateNumberById(shoppingCart);
        } else {
            //3.如果不存在，需要插入一条购物车数据

            //4.判断当前添加到购物车的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                //添加到购物车的是菜品
                //dish_id(菜品id)不为空说明添加的就是菜品，不可能是套餐因为之前说过要么添加的是菜品要么是套餐，
                //   你不可能某一次添加的购物车既是菜品又是套餐。
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                //添加到购物车的是套餐
                //这个地方不用再判断了，因为进到了else说明这个dishId一定为空，dishId为空说明
                //    这个SetmealId一定不为空。
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);//设置数量，固定第一次插入就是1.
            shoppingCart.setCreateTime(LocalDateTime.now());//创建时间
            shoppingCartMapper.insert(shoppingCart);
        }
    }
    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //查询某个用户的购物车数据，所以需要传递一个user_id
        //获取当前这个微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list =  shoppingCartMapper.list(shoppingCart);
        return list;
    }
    /**
     * 清空购物车商品：你不能删除别人的数据只能删除自己的购物车数据，
     *              所以需要有user_id作为删除条件。
     */
    @Override
    public void cleanShoppingCart() {
        //获取到当前用户的id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }
    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        //设置查询条件，查询当前登录用户的购物车数据
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if(list != null && list.size() > 0){
            shoppingCart = list.get(0);

            Integer number = shoppingCart.getNumber();
            if(number == 1){
                //当前商品在购物车中的份数为1，直接删除当前记录
                shoppingCartMapper.deleteById(shoppingCart.getId());
            }else {
                //当前商品在购物车中的份数不为1，修改份数即可
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }
}

