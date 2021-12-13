package org.wizard.marketing.core.dao;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.wizard.marketing.core.beans.CombCondition;
import org.wizard.marketing.core.beans.Condition;
import org.wizard.marketing.core.beans.EventBean;
import org.wizard.marketing.core.utils.EventUtils;

import java.util.List;

/**
 * @Author: sodamnsure
 * @Date: 2021/11/8 6:12 下午
 * @Desc: State查询器
 */
@Slf4j
public class StateQuerier {
    ListState<EventBean> listState;

    public StateQuerier(ListState<EventBean> listState) {
        this.listState = listState;
    }

    /**
     * 根据组合条件及查询的时间范围，得到返回结果的[1212]形式的字符串序列
     *
     * @param deviceId        账户ID
     * @param combCondition   行为组合条件
     * @param queryRangeStart 查询时间范围起始
     * @param queryRangeEnd   查询时间范围结束
     * @return 用户做过的组合条件中事件的字符串序列
     */
    public String getCombConditionStr(String deviceId, CombCondition combCondition, long queryRangeStart, long queryRangeEnd) throws Exception {
        // 获取state中的数据迭代器
        Iterable<EventBean> eventBeans = listState.get();
        // 获取组合条件中的感兴趣的事件
        List<Condition> conditionList = combCondition.getConditionList();
        // 迭代数据，拼接角标字符串
        StringBuilder sb = new StringBuilder();
        for (EventBean event : eventBeans) {
            if (event.getTimeStamp() >= queryRangeStart && event.getTimeStamp() <= queryRangeEnd) {
                for (int i = 0; i < conditionList.size(); i++) {
                    // 判断迭起迭代到的event，是否是条件中感兴趣的事件
                    if (EventUtils.eventMatchCondition(event, conditionList.get(i))) {
                        sb.append(i + 1);
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * 根据组合条件及查询的时间范围，查询该组合出现的次数
     *
     * @param deviceId        账户ID
     * @param combCondition   行为组合条件
     * @param queryRangeStart 查询时间范围起始
     * @param queryRangeEnd   查询时间范围结束
     * @return 出现的次数
     */
    public int getCombConditionCount(String deviceId, CombCondition combCondition, long queryRangeStart, long queryRangeEnd) throws Exception {
        // 先查询到用户在组合条件中做过的事件的字符串序列
        String eventSeqStr = getCombConditionStr(deviceId, combCondition, queryRangeStart, queryRangeEnd);
        // 调用工具，来获取事件序列与正则表达式的匹配次数--即组合条件发生的次数
        int count = EventUtils.eventSeqStrMatchRegexCount(eventSeqStr, combCondition.getMatchPattern());

        log.debug("在State中查询组合事件条件，得到的事件序列字符串: {}, 正则表达式: {}, 匹配结果: {}", eventSeqStr, combCondition.getMatchPattern(), count);
        return count;
    }
}
