# Can LLMs be used to create control loops in autonomous networks?
This project will explore whether LLMs are able to generate code for the purpose of rapidly prototyping modules for use in a CloudSim control loop. 

The key questions are:

- Can the LLM generate variable N requested modules?
  _Yes, up to 10 of each module type. 40 overall_
- Do these modules compile?
  _Yes, providing a list of well-defined API and imports allowed all modules to compile_
- Are the API semantics of these modules correct?
  _Yes, controllers are always structurally compatible, we don't get any datatype errors_ 
- Do modules remain logically distinct as we scale up request size?
  _Need to test this with MOSS_
- Can the modules be composed into working controllers?
  _Yes, we observe few crashes but many inert controllers. No data-type mmismatches _
- How do these controllers fair in simulated enviroments?
  _A lot of inert controllers but we definitelty observe winners._
