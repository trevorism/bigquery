package com.trevorism.service

import com.trevorism.model.Describe


interface DescribeService {
    List<String> describe(Describe request)
}