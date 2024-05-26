package com.trevorism.service

import com.trevorism.model.Describe


@jakarta.inject.Singleton
class InMemoryDescribeService implements DescribeService {

    @Override
    List<String> describe(Describe request) {
        ["list","create","read"]
    }
}
