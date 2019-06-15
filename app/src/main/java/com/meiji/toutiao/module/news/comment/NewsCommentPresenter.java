package com.meiji.toutiao.module.news.comment;

import android.widget.Toast;

import com.meiji.toutiao.ErrorAction;
import com.meiji.toutiao.InitApp;
import com.meiji.toutiao.api.IMobileNewsApi;
import com.meiji.toutiao.bean.news.NewsCommentBean;
import com.meiji.toutiao.util.RetrofitFactory;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Meiji on 2016/12/20.
 */

public class NewsCommentPresenter implements INewsComment.Presenter {

    private static final String TAG = "NewsCommentPresenter";
    private INewsComment.View view;
    private String groupId;
    private String itemId;
    private int offset = 0;
    private List<NewsCommentBean.DataBean.CommentBean> commentsBeanList = new ArrayList<>();
    String filterContent;

    public NewsCommentPresenter(INewsComment.View view) {
        this.view = view;
    }

    public void setFilter(String content) {
        filterContent = content;
    }


    @Override
    public void doLoadData(String... groupId_ItemId) {

        try {
            if (null == this.groupId) {
                this.groupId = groupId_ItemId[0];
            }
            if (null == this.itemId) {
                this.itemId = groupId_ItemId[1];
            }
        } catch (Exception e) {
            ErrorAction.print(e);
        }

        RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
                .getNewsComment(groupId, offset)
                .subscribeOn(Schedulers.io())
                .map(newsCommentBean -> {
                    List<NewsCommentBean.DataBean.CommentBean> data = new ArrayList<>();
                    for (NewsCommentBean.DataBean bean : newsCommentBean.getData()) {
                        data.add(bean.getComment());
                    }
                    return data;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(view.bindAutoDispose())
                .subscribe(list -> {

//                    if (filterContent!=null && !"".equals(filterContent.trim())){
//                        List<NewsCommentBean.DataBean.CommentBean> listNew = new ArrayList<>();
//                        for (int i = 0; i < list.size(); i++) {
//                            if (list.get(i).getText() != null) {
//                                if (list.get(i).getText().contains(filterContent)) {
//                                    listNew.add(list.get(i));
//                                }
//                            }
//                        }
//                        list = listNew;
//                    }
                    if (null != list && list.size() > 0) {
                        doSetAdapter(list);
                    } else {
                        doShowNoMore();
                    }
                }, throwable -> {
                    doShowNetError();
                    ErrorAction.print(throwable);
                });
    }

    @Override
    public void doLoadMoreData() {
        offset += 20;
        doLoadData();
    }

    @Override
    public void doSetAdapter(List<NewsCommentBean.DataBean.CommentBean> list) {
        if (filterContent != null && !"".equals(filterContent.trim())) {
            List<NewsCommentBean.DataBean.CommentBean> listNew = new ArrayList<>();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getText() != null) {
                        if (list.get(i).getText().contains(filterContent)) {
                            listNew.add(list.get(i));
                        }
                    }
                }
            }
            if (listNew.size() == 0) {
                Toast.makeText(InitApp.AppContext,"暂无相关内容",Toast.LENGTH_SHORT).show();

            } else {
                commentsBeanList.addAll(listNew);
            }
        } else {
            commentsBeanList.addAll(list);
        }
        view.onSetAdapter(commentsBeanList);
        view.onHideLoading();
    }

    @Override
    public void doRefresh() {
        if (commentsBeanList.size() != 0) {
            commentsBeanList.clear();
            offset = 0;
        }
        doLoadData();
    }

    @Override
    public void doShowNetError() {
        view.onHideLoading();
        view.onShowNetError();
    }

    @Override
    public void doShowNoMore() {
        view.onHideLoading();
        view.onShowNoMore();
    }
}
