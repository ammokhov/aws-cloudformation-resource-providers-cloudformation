package software.amazon.cloudformation.stack;

import software.amazon.awssdk.services.cloudformation.model.*;
import software.amazon.awssdk.services.cloudformation.model.Tag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {
    static CreateStackRequest createStackRequest(final ResourceModel model) {
        return CreateStackRequest.builder()
                .stackName(model.getStackName())
                .disableRollback(true)
                .notificationARNs(model.getNotificationARNs())
                .parameters(translateParameters(model.getParameters()))
                .tags(translateTagsToSdk(model.getTags()))
                .templateURL(model.getTemplateURL())
                .timeoutInMinutes(model.getTimeoutInMinutes())
                .build();
    }

    static DeleteStackRequest deleteStackRequest(final ResourceModel model) {
        return DeleteStackRequest.builder()
                .stackName(model.getStackName()).build();
    }

    static DescribeStacksRequest describeStacksRequest(final ResourceModel model) {
        return DescribeStacksRequest.builder()
                .stackName(model.getId()).build();
    }

    static List<Parameter> translateParameters(final Map<String, Object> parameters) {
        return parameters.entrySet()
                .stream()
                .map(parameter ->
                        Parameter.builder()
                                .parameterKey(parameter.getKey())
                                .parameterValue((String) parameter.getValue())
                                .build())
                .collect(Collectors.toList());
    }

    static List<Tag> translateTagsToSdk(final Set<software.amazon.cloudformation.stack.Tag> tags) {
        if (tags == null) return null;
        return tags.stream()
                .collect(Collectors.mapping(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build(),
                        Collectors.toList()));
    }


}
